<?php

namespace pocketcloud\cloud\http\route;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\socket\auth\Authentication;
use pocketcloud\cloud\http\util\StatusCode;

abstract class RegularPath implements Path {

    public function __construct(
        private readonly string $path,
        private readonly string $requestMethod,
        private readonly Authentication $authentication
    ) {}

    public function handleFailedAuth(Request $request): Response {
        return ResponseBuilder::create()
            ->code(StatusCode::FORBIDDEN)
            ->build();
    }

    final public function getApiVersion(): ?string {
        return null;
    }

    public function getPath(): string {
        return $this->path;
    }

    public function getFullPath(): string {
        return "/" . trim($this->getPath(), "/");
    }

    public function getMethod(): string {
        return $this->requestMethod;
    }

    public function getAuthentication(): Authentication {
        return $this->authentication;
    }
}