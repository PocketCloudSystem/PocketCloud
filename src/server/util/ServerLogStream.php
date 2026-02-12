<?php

namespace pocketcloud\cloud\server\util;

use pocketcloud\cloud\server\CloudServer;
use RuntimeException;

final class ServerLogStream {

    private bool $startedStream = false;
    private mixed $fileHandle = null;

    public function __construct(private readonly CloudServer $server) {}

    public function startStream(): void {
        if (!@file_exists($this->server->getLogFilePath())) throw new RuntimeException("Log file does not exist");
        $fileHandle = fopen($this->server->getLogFilePath(), "r");
        if ($fileHandle === false) throw new RuntimeException("Log file cannot be opened");
        $this->fileHandle = $fileHandle;
        $this->startedStream = true;
    }

    public function readNewLine(): string|false|null {
        if (!$this->startedStream) return false;
        $line = fgets($this->fileHandle);
        if ($line === false) {
            clearstatcache(false, $this->server->getLogFilePath());
            $currentPos = ftell($this->fileHandle);
            fseek($this->fileHandle, $currentPos);
            $line = fgets($this->fileHandle);
            if ($line === false) return null;
        }

        return trim($line);
    }

    public function stopStream(): void {
        if ($this->startedStream && is_resource($this->fileHandle)) {
            fclose($this->fileHandle);
        }

        $this->startedStream = false;
        $this->fileHandle = null;
    }
}