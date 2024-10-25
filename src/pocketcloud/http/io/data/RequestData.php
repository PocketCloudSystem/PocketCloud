<?php

namespace pocketcloud\http\io\data;

use pocketcloud\util\Address;

readonly class RequestData {
	
	public function __construct(
        private Address $requestAddress,
        private string $method,
        private string $path,
        private Queries $queries
    ) {}

	public function address(): Address {
		return $this->requestAddress;
	}

	public function method(): string {
		return $this->method;
	}

	public function path(): string {
		return $this->path;
	}

	public function queries(): Queries {
		return $this->queries;
	}
}