<?php

namespace pocketcloud\http\io\data;

use pocketcloud\util\Address;

class RequestData {
	
	public function __construct(
        protected Address $requestAddress,
        protected string $method,
        protected string $path,
        protected Queries $queries
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