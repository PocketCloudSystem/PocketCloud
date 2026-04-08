<?php

namespace pocketcloud\cloud\http\client\thread\misc;

use pmmp\thread\ThreadSafe;
use pocketcloud\cloud\http\client\exception\ReconstructedException;
use Throwable;

final class RequestExceptionData extends ThreadSafe {

    public function __construct(
        public readonly string $message,
        public readonly int $code,
        public readonly string $file,
        public readonly int $line,
        public readonly string $trace
    ) {}

    public function toException(): Throwable {
        return new ReconstructedException($this->message, $this->code, $this->file, $this->line, $this->trace);
    }

    public function write(): array {
        return [
            "message" => $this->message,
            "code" => $this->code,
            "file" => $this->file,
            "line" => $this->line,
            "trace" => $this->trace
        ];
    }

    public function read(array $data): self {
        return new self(...$data);
    }

    public static function fromException(Throwable $e): self {
        return new self($e->getMessage(), $e->getCode(), $e->getFile(), $e->getLine(), $e->getTraceAsString());
    }

    public static function create(string $message, int $code, string $file, int $line, string $trace): self {
        return new self($message, $code, $file, $line, $trace);
    }
}