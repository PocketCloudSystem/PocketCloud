<?php

namespace pocketcloud\cloud\http\io;

use pocketcloud\cloud\http\route\Path;
use pocketcloud\cloud\util\net\Address;

final readonly class Request {

	public function __construct(
        private Address $address,
        private string $method,
        private Path $path,
        private array $queries,
        private array $headers,
        protected ?string $body = null
    ) {}

    public function hasQuery(string $key): bool {
        return isset($this->queries[$key]);
    }

    public function hasHeader(string $key): bool {
        return isset($this->headers[$key]);
    }

    public function getQuery(string $key, mixed $default = null): mixed {
        return $this->queries[$key] ?? $default;
    }

    public function getHeader(string $key, mixed $default = null): mixed {
        return $this->headers[$key] ?? $default;
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public function getMethod(): string {
        return $this->method;
    }

    public function getPath(): Path {
        return $this->path;
    }

    public function getQueries(bool $sorted = false): array {
        $queries = $this->queries;
        if ($sorted) ksort($queries);
        return $queries;
    }

    public function getHeaders(): array {
        return $this->headers;
    }

    public function getBody(): ?string {
        return $this->body;
    }
}