<?php

namespace pocketcloud\cloud\server\data;

use LogicException;
use pocketcloud\cloud\console\log\CloudLogger;

final class CloudServerData {

    private ?int $tempProcessId = null;

    public function __construct(
        private readonly string $serverName,
        private readonly int $port,
        private int $maxPlayers,
        private ?int $processId = null
    ) {}

    public function setMaxPlayers(int $maxPlayers): void {
        $this->maxPlayers = $maxPlayers;
    }

    public function setTempProcessId(?int $tempProcessId): void {
        if ($this->tempProcessId !== null) throw new LogicException("The temp process id has already been set");
        CloudLogger::get()->forceDebug("Set temp process id of {} to {}", $this->serverName, $tempProcessId ?? "NULL");
        $this->tempProcessId = $tempProcessId;
    }

    public function setProcessId(?int $processId): void {
        if ($this->processId !== null) throw new LogicException("The process id has already been set");
        $this->processId = $processId;
    }

    public function getPort(): int {
        return $this->port;
    }

    public function getMaxPlayers(): int {
        return $this->maxPlayers;
    }

    public function getProcessId(): ?int {
        return $this->processId ?? $this->tempProcessId;
    }
}