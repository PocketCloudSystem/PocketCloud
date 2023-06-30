<?php

namespace pocketcloud\server\data;

class CloudServerData {

    public function __construct(
        private readonly int $port,
        private readonly int $maxPlayers,
        private ?int $processId = null
    ) {}

    public function getPort(): int {
        return $this->port;
    }

    public function getMaxPlayers(): int {
        return $this->maxPlayers;
    }

    public function getProcessId(): ?int {
        return $this->processId;
    }

    public function setProcessId(?int $processId): void {
        $this->processId = $processId;
    }
}