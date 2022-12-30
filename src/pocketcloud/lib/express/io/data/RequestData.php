<?php

namespace pocketcloud\lib\express\io\data;

use pocketcloud\lib\express\utils\Collection;
use pocketcloud\utils\Address;

class RequestData {
	
	public function __construct(protected Address $requestAddress, protected string $method, protected string $path, protected Collection $queries) { }
	
	/**
	 * @return Address
	 */
	public function address(): Address {
		return $this->requestAddress;
	}
	
	/**
	 * @return string
	 */
	public function method(): string {
		return $this->method;
	}
	
	/**
	 * @return string
	 */
	public function path(): string {
		return $this->path;
	}
	
	/**
	 * @return Collection
	 */
	public function queries(): Collection {
		return $this->queries;
	}
}