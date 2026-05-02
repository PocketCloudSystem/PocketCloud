<?php

namespace pocketcloud\cloud\http\server\util;

use pmmp\thread\ThreadSafe;

final class PendingResponse extends ThreadSafe {

    public function __construct(
        private readonly string $clientId,
        private readonly string $httpResponse
    ) {}

    public function getClientId(): string {
        return $this->clientId;
    }

    public function getHttpResponse(): string {
        return $this->httpResponse;
    }
}