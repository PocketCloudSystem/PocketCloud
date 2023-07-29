<?php

namespace pocketcloud\http\io\data;

class Queries {

    public function __construct(private readonly array $queries) {}

    public function get(string $key, mixed $default = null): mixed {
        return $this->queries[$key] ?? $default;
    }

    public function has(string $key): bool {
        return isset($this->queries[$key]);
    }

    public function all(): array {
        return $this->queries;
    }
}