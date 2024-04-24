<?php

namespace pocketcloud\http\io;

use pocketcloud\config\impl\DefaultConfig;
use pocketcloud\http\io\data\RequestData;
use stdClass;

class Request extends stdClass {

	public const SUPPORTED_REQUEST_METHODS = ["GET", "POST", "PUT", "DELETE", "PATCH"];

	public function __construct(
        private readonly array $headers,
        private readonly RequestData $requestData,
        private readonly ?string $body = null
    ) {}

    public function authorized(): bool {
        return isset($this->headers["auth-key"]) && $this->headers["auth-key"] == DefaultConfig::getInstance()->getHttpServerAuthKey();
    }

	public function getBody(): ?string {
		return $this->body;
	}

	public function getHeaders(): array {
		return $this->headers;
	}

	public function data(): RequestData {
		return $this->requestData;
	}
}