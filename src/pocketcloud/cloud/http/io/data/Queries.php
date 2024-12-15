<?php

namespace pocketcloud\cloud\http\io\data;

final readonly class Queries {

    public function __construct(private array $queries) {}

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