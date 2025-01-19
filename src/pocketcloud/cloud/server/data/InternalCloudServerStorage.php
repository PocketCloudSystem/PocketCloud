<?php

namespace pocketcloud\cloud\server\data;

use pocketcloud\cloud\network\packet\impl\normal\CloudSyncStoragesPacket;
use pocketcloud\cloud\server\CloudServer;

final class InternalCloudServerStorage {

    public function __construct(
        private readonly CloudServer $server,
        private array $storage = []
    ) {}

    /** @internal  */
    public function sync(array $data): void {
        $this->storage = $data;
    }

    private function outgoingSync(): void {
        CloudSyncStoragesPacket::create()->broadcastPacket();
    }

    public function set(string $k, mixed $v): self {
        $this->storage[$k] = $v;
        $this->outgoingSync();
        return $this;
    }

    public function remove(string $k): self {
        if (isset($this->storage[$k])) {
            unset($this->storage[$k]);
            $this->outgoingSync();
        }
        return $this;
    }

    public function has(string $k): bool {
        return isset($this->storage[$k]);
    }

    public function get(string $k, mixed $default = null): mixed {
        return $this->storage[$k] ?? $default;
    }

    public function clear(): self {
        $this->storage = [];
        $this->outgoingSync();
        return $this;
    }

    public function empty(): bool {
        return empty($this->storage);
    }

    public function server(): CloudServer {
        return $this->server;
    }

    public function getAll(): array {
        return $this->storage;
    }
}