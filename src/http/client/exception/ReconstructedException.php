<?php

namespace pocketcloud\cloud\http\client\exception;

use Exception;

final class ReconstructedException extends Exception {

    public function __construct(
        string $message,
        int $code,
        private readonly string $originalFile,
        private readonly int $originalLine,
        private readonly string $originalTrace
    ) {
        parent::__construct($message, $code);
    }

    public function getOriginalFile(): string {
        return $this->originalFile;
    }

    public function getOriginalLine(): int {
        return $this->originalLine;
    }

    public function getOriginalTrace(): string {
        return $this->originalTrace;
    }
}