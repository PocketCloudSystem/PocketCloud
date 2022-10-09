<?php

namespace pocketcloud\network\client;

use pocketcloud\server\CloudServer;
use pocketcloud\server\CloudServerManager;
use pocketcloud\utils\SingletonTrait;

class ServerClientManager {
    use SingletonTrait;

    /** @var array<ServerClient> */
    private array $clients = [];

    public function addClient(CloudServer $server, ServerClient $client) {
        if (!isset($this->clients[$server->getName()])) $this->clients[$server->getName()] = $client;
    }

    public function removeClient(CloudServer $server) {
        if (isset($this->clients[$server->getName()])) unset($this->clients[$server->getName()]);
    }

    public function checkClient(ServerClient $client): bool {
        return in_array($client, $this->clients);
    }

    public function checkServer(CloudServer $server): bool {
        return isset($this->clients[$server->getName()]);
    }

    public function getClientOfServer(CloudServer $server): ?ServerClient {
        return $this->clients[$server->getName()] ?? null;
    }

    public function getServerOfClient(ServerClient $client): ?CloudServer {
        return in_array($client, $this->clients) ? CloudServerManager::getInstance()->getServerByName(array_search($client, $this->clients)) : null;
    }

    public function getClients(): array {
        return $this->clients;
    }
}