<?php

namespace pocketcloud\cloud\http\io;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\http\io\data\RequestData;
use stdClass;

final class Request extends stdClass {

	public const array SUPPORTED_REQUEST_METHODS = ["GET", "POST", "PUT", "DELETE", "PATCH"];

	public function __construct(
        private readonly array $headers,
        private readonly RequestData $requestData,
        private readonly ?string $body = null
    ) {}

    public function authorized(): bool {
        return isset($this->headers["auth-key"]) && $this->headers["auth-key"] == MainConfig::getInstance()->getHttpServerAuthKey();
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