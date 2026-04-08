<?php

namespace pocketcloud\cloud\http\client;

use pocketcloud\cloud\util\net\Address;

final class HttpClientBuilder {

    public static function create(Address $baseAddress): self {
        return new self($baseAddress);
    }

    /**
     * @param Address $baseAddress
     * @param string|null $basePath
     * @param array $defaultHeaders
     * @param float $timeout Request timeout in seconds
     * @param int $retries Retries after socket error
     */
    public function __construct(
        private Address $baseAddress,
        private ?string $basePath = null,
        private array $defaultHeaders = [],
        private float $timeout = 5.0,
        private int $retries = 1
    ) {}

    public function baseAddress(Address $baseAddress): self {
        $this->baseAddress = $baseAddress;
        return $this;
    }

    public function basePath(?string $basePath): self {
        $this->basePath = $basePath;
        return $this;
    }

    public function defaultHeaders(array $defaultHeaders): self {
        $this->defaultHeaders = $defaultHeaders;
        return $this;
    }

    public function defaultHeader(string $name, string $value): self {
        $this->defaultHeaders[$name] = $value;
        return $this;
    }

    public function timeout(float $timeout): self {
        $this->timeout = $timeout;
        return $this;
    }

    public function retries(int $retries): self {
        if ($retries < 0) $retries = 0;
        $this->retries = $retries;
        return $this;
    }

    public function build(): HttpClient {
        return new HttpClient($this->baseAddress, $this->basePath, $this->defaultHeaders, $this->timeout, $this->retries);
    }
}