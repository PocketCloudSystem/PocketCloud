<?php

namespace pocketcloud\cloud\server\data;

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

    public function set(string $k, mixed $v): self {
        if (!isset($this->storage[$k])) {
            $this->storage[$k] = $v;
            //sync
        }
        return $this;
    }

    public function remove(string $k): self {
        if (isset($this->storage[$k])) {
            unset($this->storage[$k]);
            //sync
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
        //TODO: sync
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