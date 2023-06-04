<?php

namespace pocketcloud\http\io;

use pocketcloud\config\DefaultConfig;
use pocketcloud\http\io\data\RequestData;
use stdClass;

class Request extends stdClass {

	public const SUPPORTED_REQUEST_METHODS = ["GET", "POST", "PUT", "DELETE", "PATCH"];

	public function __construct(
        protected array $headers,
        protected RequestData $requestData,
        protected ?string $body = null
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