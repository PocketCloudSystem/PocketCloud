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
        private ?int $processId = null,
        private float $tps = -1,
        private float $avgTps = -1,
        private float $memoryUsage = -1,
        private float $memoryPeak = -1,
        private float $memoryLimit = -1,
        private float $cpuUsage = -1
    ) {}

    public function setMaxPlayers(int $maxPlayers): void {
        $this->maxPlayers = $maxPlayers;
    }

    public function setTempProcessId(?int $tempProcessId): void {
        if ($this->tempProcessId !== null) throw new LogicException("The temp process id has already been set");
        CloudLogger::get()->debug("Set temp process id of {} to {}", $this->serverName, $tempProcessId ?? "NULL");
        $this->tempProcessId = $tempProcessId;
    }

    public function setProcessId(?int $processId): void {
        if ($this->processId !== null) throw new LogicException("The process id has already been set");
        CloudLogger::get()->debug("Set process id of {} to {}, matches temp process id?: {}", $this->serverName, $processId ?? "NULL", $this->tempProcessId !== null && $this->tempProcessId === $processId ? "Yes" : "No");
        $this->processId = $processId;
    }

    public function setPerformanceStats(float $tps, float $avgTps, float $memoryUsage, float $memoryPeak, float $memoryLimit, float $cpuUsage): void {
        $this->tps = $tps;
        $this->avgTps = $avgTps;
        $this->memoryUsage = $memoryUsage;
        $this->memoryPeak = $memoryPeak;
        $this->memoryLimit = $memoryLimit;
        $this->cpuUsage = $cpuUsage;
    }

    public function setTps(float $tps): void {
        $this->tps = $tps;
    }

    public function setAvgTps(float $avgTps): void {
        $this->avgTps = $avgTps;
    }

    public function setMemoryUsage(float $memoryUsage): void {
        $this->memoryUsage = $memoryUsage;
    }

    public function setMemoryPeak(float $memoryPeak): void {
        $this->memoryPeak = $memoryPeak;
    }

    public function setMemoryLimit(float $memoryLimit): void {
        $this->memoryLimit = $memoryLimit;
    }

    public function setCpuUsage(float $cpuUsage): void {
        $this->cpuUsage = $cpuUsage;
    }

    public function getServerName(): string {
        return $this->serverName;
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

    public function getTps(): float {
        return $this->tps;
    }

    public function getAvgTps(): float {
        return $this->avgTps;
    }

    public function getMemoryUsage(): float {
        return $this->memoryUsage;
    }

    public function getMemoryPeak(): float {
        return $this->memoryPeak;
    }

    public function getMemoryLimit(): float {
        return $this->memoryLimit;
    }

    public function getCpuUsage(): float {
        return $this->cpuUsage;
    }
}