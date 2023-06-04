<?php

namespace pocketcloud\server\storage;

use pocketcloud\server\CloudServer;

class CloudServerStorage {

    public function __construct(
        private CloudServer $server,
        private array $storage = []
    ) {}

    public function put(string $k, mixed $v): self {
        if (!isset($this->storage[$k])) $this->storage[$k] = $v;
        return $this;
    }

    public function remove(string $k): self {
        if (isset($this->storage[$k])) unset($this->storage[$k]);
        return $this;
    }

    public function has(string $k): bool {
        return isset($this->storage[$k]);
    }

    public function get(string $k, mixed $default = null): mixed {
        return $this->storage[$k] ?? $default;
    }

    public function replace(string $k, mixed $v): self {
        if (isset($this->storage[$k])) $this->storage[$k] = $v;
        return $this;
    }

    public function clear(): self {
        $this->storage = [];
        return $this;
    }

    public function getServer(): CloudServer {
        return $this->server;
    }

    public function getStorage(): array {
        return $this->storage;
    }
}