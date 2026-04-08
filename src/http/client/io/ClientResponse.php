<?php

namespace pocketcloud\cloud\http\client\io;

use pocketcloud\cloud\http\client\util\RequestMethod;
use pocketcloud\cloud\http\client\util\StatusCode;
use Throwable;

final readonly class ClientResponse {

    public function __construct(
        private string $url,
        private RequestMethod $requestMethod,
        private StatusCode $statusCode,
        private array $headers,
        private ?string $body,
        private mixed $parsedBody,
        private int $takenRetries,
        private ?Throwable $exception = null
    ) {}

    public function isInformational(): bool {
        return $this->statusCode->isInformational();
    }

    public function isSuccess(): bool {
        return $this->statusCode->isSuccess();
    }

    public function isRedirection(): bool {
        return $this->statusCode->isRedirection();
    }

    public function isClientError(): bool {
        return $this->statusCode->isClientError();
    }

    public function isServerError(): bool {
        return $this->statusCode->isServerError();
    }

    public function crashed(): bool {
        return $this->exception !== null;
    }

    public function url(): string {
        return $this->url;
    }

    public function requestMethod(): RequestMethod {
        return $this->requestMethod;
    }

    public function statusCode(): StatusCode {
        return $this->statusCode;
    }

    public function getHeader(string $key, mixed $default = null): mixed {
        return $this->headers[$key] ?? $default;
    }

    public function headers(): array {
        return $this->headers;
    }

    public function bodyRaw(): ?string {
        return $this->body;
    }

    public function body(): mixed {
        return $this->parsedBody ?? $this->body;
    }

    public function takenRetries(): int {
        return $this->takenRetries;
    }

    public function exception(): ?Throwable {
        return $this->exception;
    }

    public function withException(Throwable $exception): self {
        return new self(
            $this->url,
            $this->requestMethod,
            $this->statusCode,
            $this->headers,
            $this->body,
            $this->parsedBody,
            $this->takenRetries,
            $exception
        );
    }

    public static function fromException(ClientRequestContext $context, Throwable $exception, ?ClientResponse $response = null): self {
        return new self(
            $context->finalUrl() ?? $context->url(),
            $context->method(),
            StatusCode::INTERNAL_SERVER_ERROR,
            $response?->headers() ?? [],
            $response?->bodyRaw() ?? null,
            $response?->body() ?? null,
            $response?->takenRetries() ?? 0,
            $exception
        );
    }
}