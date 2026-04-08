<?php

namespace pocketcloud\cloud\http\server\util;

use pmmp\thread\ThreadSafe;
use pocketcloud\cloud\util\net\Address;

final class UnhandledHttpRequest extends ThreadSafe {

    public function __construct(
        private readonly string $buffer,
        private readonly Address $address,
        private readonly string $clientId,
        private readonly int $bufferSize,
        private readonly int $contentLength
    ) {}

    public function getBuffer(): string {
        return $this->buffer;
    }

    public function getBufferSize(): int {
        return $this->bufferSize;
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public function getClientId(): string {
        return $this->clientId;
    }

    public function getContentLength(): int {
        return $this->contentLength;
    }
}