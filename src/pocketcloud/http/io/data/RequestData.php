<?php

namespace pocketcloud\http\io\data;

use pocketcloud\util\Address;

class RequestData {
	
	public function __construct(
        private readonly Address $requestAddress,
        private readonly string $method,
        private readonly string $path,
        private readonly Queries $queries
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