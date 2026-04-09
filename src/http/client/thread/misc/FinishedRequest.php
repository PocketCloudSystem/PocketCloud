<?php

namespace pocketcloud\cloud\http\client\thread\misc;

use pmmp\thread\ThreadSafe;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\http\client\io\ClientResponse;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\http\util\StatusCode;

final class FinishedRequest extends ThreadSafe {

    public function __construct(
        public readonly string $requestId,
        public readonly string $url,
        public readonly string $requestMethod,
        public readonly int $statusCode,
        public readonly ThreadSafeArray $headers,
        public readonly ?string $body,
        public readonly mixed $parsedBody,
        public readonly int $takenRetries,
        public readonly ?RequestExceptionData $exception
    ) {}

    public function toClientResponse(): ClientResponse {
        return new ClientResponse(
            $this->url,
            RequestMethod::fromName($this->requestMethod) ?? RequestMethod::GET,
            StatusCode::tryFrom($this->statusCode) ?? StatusCode::UNKNOWN,
            (array) $this->headers,
            $this->body,
            $this->parsedBody === null ? null : unserialize($this->parsedBody),
            $this->takenRetries,
            $this->exception?->toException()
        );
    }
}