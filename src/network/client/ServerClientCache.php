<?php

namespace pocketcloud\cloud\network\client;

use Closure;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class ServerClientCache implements Tickable {
    use SingletonTrait;

    /** @var array<ServerClient> */
    private array $clients = [];
    /** @var array<string> */
    private array $servers = [];
    /** @var array<ServerClient> */
    private array $clientsByAddress = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function add(CloudServer $server, ServerClient $client): void {
        if (!$this->isset($client)) {
            CloudLogger::get()->debug("Adding client {} => {}", $client, $server->getName());
            $this->clients[$server->getName()] = $client;
            $this->servers[$client->toString()] = $server->getName();
            $this->clientsByAddress[$client->getAddress()->toString()] = $client;
        }
    }

    public function remove(ServerClient|CloudServer $client): void {
        $client = $client instanceof CloudServer ? $this->get($client) : $client;
        if ($client !== null) {
            if ($this->isset($client)) {
                CloudLogger::get()->debug("Removing client {}", $client);
                $serverName = $this->servers[$client->toString()] ?? null;
                if ($serverName === null) {
                    unset($this->clients[array_search($client, $this->clients)]);
                } else {
                    unset($this->clients[$serverName]);
                }

                unset($this->servers[$client->toString()]);
                unset($this->clientsByAddress[$client->getAddress()->toString()]);
            }
        }
    }

    public function isset(ServerClient $client): bool {
        return isset($this->servers[$client->toString()]);
    }

    public function pick(Closure $conditionClosure): array {
        return array_filter($this->clients, $conditionClosure);
    }

    public function tick(int $currentTick): void {
        foreach ($this->clients as $client) {
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
        $serverName = $this->servers[$client->toString()] ?? null;
        if ($serverName === null) return null;
        return CloudServerManager::getInstance()->get($serverName);
    }

    public function getByAddress(Address $address): ?ServerClient {
        return $this->clientsByAddress[$address->toString()] ?? null;
    }

    public function getAll(TemplateType ...$objects): array {
        if (!empty($objects)) return array_filter($this->clients, function (ServerClient $client) use($objects): bool {
            if ($client->getServer() === null) return false;
            return array_any($objects, fn(TemplateType $object) => $object->getName() === $client->getServer()->getTemplate()->getTemplateType()->getName());
        });

        return $this->clients;
    }
}