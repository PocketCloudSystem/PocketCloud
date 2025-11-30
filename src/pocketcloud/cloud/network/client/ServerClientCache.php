<?php

namespace pocketcloud\cloud\network\client;

use Closure;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\SingletonTrait;
use pocketcloud\cloud\util\tick\Tickable;

final class ServerClientCache implements Tickable {
    use SingletonTrait;

    /** @var array<ServerClient> */
    private array $clients = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function add(CloudServer $server, ServerClient $client): void {
        if (!$this->isset($client)) {
            CloudLogger::get()->debug("Adding client " . $client->getAddress() . " => " . $server->getName());
            $this->clients[$server->getName()] = $client;
        }
    }

    public function remove(ServerClient|CloudServer $client): void {
        $client = $client instanceof CloudServer ? $this->get($client) : $client;
        if ($client !== null) {
            if ($this->isset($client)) {
                CloudLogger::get()->debug("Removing client " . $client->getAddress());
                unset($this->clients[array_search($client, $this->clients)]);
            }
        }
    }

    public function isset(ServerClient $client): bool {
        return in_array($client, $this->clients);
    }

    public function pick(Closure $conditionClosure): array {
        return array_filter($this->clients, $conditionClosure);
    }

    public function tick(int $currentTick): void {
        $clientsWithDelayedPackets = array_filter($this->clients, fn(ServerClient $client) => count($client->getDelayedPackets()) > 0);
        if (count($clientsWithDelayedPackets) == 0) return;
        foreach ($clientsWithDelayedPackets as $client) {
            foreach ($client->getDelayedPackets() as $i => $data) {
                $packet = $data[0];
                $tick = $data[1];
                if ($tick <= $currentTick) {
                    $sendClosure = $data[2] ?? null;
                    $success = $client->sendPacket($packet);
                    if ($sendClosure !== null) ($sendClosure)($client, $packet, $success);
                    $client->unsetDelayedPacket($i);
                }
            }
        }
    }

    public function get(CloudServer $server): ?ServerClient {
        return $this->clients[$server->getName()] ?? null;
    }

    public function getServer(ServerClient $client): ?CloudServer {
        return $this->isset($client) ? CloudServerManager::getInstance()->get(array_search($client, $this->clients)) : null;
    }

    public function getByAddress(Address $address): ?ServerClient {
        return array_find($this->clients, fn($client) => $client->getAddress()->equals($address));
    }

    public function getAll(): array {
        return $this->clients;
    }
}