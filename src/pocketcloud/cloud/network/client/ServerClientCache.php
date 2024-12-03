<?php

namespace pocketcloud\cloud\network\client;

use Closure;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\SingletonTrait;

final class ServerClientCache {
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

    public function get(CloudServer $server): ?ServerClient {
        return $this->clients[$server->getName()] ?? null;
    }

    public function getServer(ServerClient $client): ?CloudServer {
        return $this->isset($client) ? CloudServerManager::getInstance()->get(array_search($client, $this->clients)) : null;
    }

    public function getByAddress(Address $address): ?ServerClient {
        foreach ($this->clients as $client) if ($client->getAddress()->equals($address)) return $client;
        return null;
    }

    public function getAll(): array {
        return $this->clients;
    }
}