<?php

namespace pocketcloud\server\storage;

use pocketcloud\network\Network;
use pocketcloud\network\packet\impl\normal\CloudSyncStoragesPacket;
use pocketcloud\server\CloudServer;

final class CloudServerStorage {

    public function __construct(
        private readonly CloudServer $server,
        private array $storage = []
    ) {}

    /** @internal  */
    public function sync(array $data): void {
        $this->storage = $data;
    }

    public function put(string $k, mixed $v): self {
        if (!isset($this->storage[$k])) {
            $this->storage[$k] = $v;
            Network::getInstance()->broadcastPacket(new CloudSyncStoragesPacket());
        }
        return $this;
    }

    public function remove(string $k): self {
        if (isset($this->storage[$k])) {
            unset($this->storage[$k]);
            Network::getInstance()->broadcastPacket(new CloudSyncStoragesPacket());
        }
        return $this;
    }

    public function has(string $k): bool {
        return isset($this->storage[$k]);
    }

    public function get(string $k, mixed $default = null): mixed {
        return $this->storage[$k] ?? $default;
    }

    public function replace(string $k, mixed $v): self {
        if (isset($this->storage[$k])) {
            $this->storage[$k] = $v;
            Network::getInstance()->broadcastPacket(new CloudSyncStoragesPacket());
        }
        return $this;
    }

    public function clear(): self {
        $this->storage = [];
        Network::getInstance()->broadcastPacket(new CloudSyncStoragesPacket());
        return $this;
    }

    public function empty(): bool {
        return empty($this->storage);
    }

    public function getServer(): CloudServer {
        return $this->server;
    }

    public function getStorage(): array {
        return $this->storage;
    }
}