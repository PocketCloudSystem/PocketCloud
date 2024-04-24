<?php

namespace pocketcloud\server\data;

class CloudServerData {

    public function __construct(
        private readonly int $port,
        private int $maxPlayers,
        private ?int $processId = null
    ) {}

    public function setMaxPlayers(int $maxPlayers): void {
        $this->maxPlayers = $maxPlayers;
    }

    public function setProcessId(?int $processId): void {
        $this->processId = $processId;
    }

    public function getPort(): int {
        return $this->port;
    }

    public function getMaxPlayers(): int {
        return $this->maxPlayers;
    }

    public function getProcessId(): ?int {
        return $this->processId;
    }
}