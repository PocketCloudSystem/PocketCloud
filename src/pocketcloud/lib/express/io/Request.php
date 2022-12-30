<?php

namespace pocketcloud\lib\express\io;

use pocketcloud\lib\express\io\data\RequestData;
use pocketcloud\lib\express\utils\Collection;
use stdClass;

class Request extends stdClass {

	public const SUPPORTED_REQUEST_METHODS = ["GET", "POST", "PUT", "DELETE", "PATCH"];
	
	/**
	 * @param Collection $headers
	 * @param RequestData $requestData
	 * @param string|null $body
	 */
	public function __construct(protected Collection $headers, protected RequestData $requestData, protected ?string $body = null) { }
	
	/**
	 * @return string|null
	 */
	public function getBody(): ?string {
		return $this->body;
	}
	
	/**
	 * @return Collection
	 */
	public function getHeaders(): Collection {
		return $this->headers;
	}
	
	/**
	 * @return RequestData
	 */
	public function data(): RequestData {
		return $this->requestData;
	}
}