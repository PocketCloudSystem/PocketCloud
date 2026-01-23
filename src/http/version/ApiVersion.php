<?php

namespace pocketcloud\cloud\http\version;

use pocketcloud\cloud\http\socket\auth\Authentication;

class ApiVersion {

    /**
     * @param string $version
     * @param Authentication $authentication
     * @param array $paths the array of paths (string)
     */
    public function __construct(
        private readonly string $version,
        private readonly Authentication $authentication,
        private array $paths = []
    ) {}

    public function addPath(string $method, string $path): void {
        $this->paths[$method][] = "/" . trim($path, "/");
    }

    public function getVersion(): string {
        return $this->version;
    }

    public function getAuthentication(): Authentication {
        return $this->authentication;
    }

    public function isValidPath(string $method, string $path): bool {
        $path = "/" . trim(str_replace($this->getVersion() . "/", "", $path), "/");
        return in_array($path, $this->paths[$method] ?? []);
    }

    public function getPaths(): array {
        return $this->paths;
    }
}