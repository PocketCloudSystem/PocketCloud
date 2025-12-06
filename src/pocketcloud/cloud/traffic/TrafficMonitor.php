<?php

namespace pocketcloud\cloud\traffic;

use Closure;

abstract class TrafficMonitor {

    public const string REGULAR_MODE_IN = "in";
    public const string REGULAR_MODE_OUT = "out";
    public const string SUFFIX_AVG = "_avg";

    protected bool $active = true;
    protected int $timestamp;
    protected ?int $monitoringDuration = null;

    /** @var array<array<Closure>> */
    protected array $handlers = [];

    protected int $totalBytesIn = 0;
    protected int $totalBytesOut = 0;
    protected array $byteHistoryIn = [];
    protected array $byteHistoryOut = [];

    protected ?Closure $stopMonitoringHandler = null;

    public function __construct(protected readonly string $monitorType) {
        $this->timestamp = time();
    }

    protected function addHandler(string $mode, Closure $handler): void {
        if (!isset($this->handlers[$mode])) $this->handlers[$mode] = [];
        $this->handlers[$mode][] = $handler;
    }

    /**
     * @param Closure $handler function (string $buffer, int $bytesIn, Address $source): void;
     * @return self
     */
    public function monitorIn(Closure $handler): self {
        $this->addHandler(self::REGULAR_MODE_IN, $handler);
        return $this;
    }

    /**
     * @param Closure $handler function (string $buffer, int $bytesOut, Address $destination): void;
     * @return self
     */
    public function monitorOut(Closure $handler): self {
        $this->addHandler(self::REGULAR_MODE_OUT, $handler);
        return $this;
    }

    /** @internal */
    public function pushBytes(string $mode, int $bytes): void {
        if (!$this->active) return;
        $now = microtime(true);
        switch (strtolower($mode)) {
            case self::REGULAR_MODE_IN: {
                $this->totalBytesIn += $bytes;
                $this->byteHistoryIn[] = [$now, $bytes];
                break;
            }
            case self::REGULAR_MODE_OUT: {
                $this->totalBytesOut += $bytes;
                $this->byteHistoryOut[] = [$now, $bytes];
                break;
            }
        }
    }

    /** @internal */
    public function cleanupHistory(): void {
        $now = microtime(true);
        $threshold = $now - 1;

        foreach ($this->byteHistoryIn as $i => $data) {
            if ($data[0] < $threshold) unset($this->byteHistoryIn[$i]);
        }

        foreach ($this->byteHistoryOut as $i => $data) {
            if ($data[0] < $threshold) unset($this->byteHistoryOut[$i]);
        }
    }

    public function callHandlers(string $mode, mixed ...$args): void {
        if (!$this->active) return;
        if (isset($this->handlers[$mode])) {
            foreach ($this->handlers[$mode] as $handler) {
                $handler->call($this, ...$args);
            }
        }
    }

    public function registerStopMonitoringHandler(?Closure $handler): void {
        $this->stopMonitoringHandler = $handler;
    }

    public function onStopMonitoring(mixed ...$args): bool {
        return false;
    }

    final public function stopMonitoring(mixed ...$args): void {
        if (!$this->active) return;
        $this->active = false;
        $this->handlers = [];
        $this->monitoringDuration = time() - $this->timestamp;
        TrafficMonitorManager::getInstance()->removeTrafficMonitor($this);
        if (!$this->onStopMonitoring(...$args) && $this->stopMonitoringHandler !== null) ($this->stopMonitoringHandler)(...$args);
    }

    public function isActive(): bool {
        return $this->active;
    }

    public function getTimestamp(): int {
        return $this->timestamp;
    }

    public function getMonitoringDuration(): int {
        if ($this->monitoringDuration === null) return time() - $this->timestamp;
        return $this->monitoringDuration;
    }

    public function getHandlers(): array {
        return $this->handlers;
    }

    public function getTotalBytesIn(): int {
        return $this->totalBytesIn;
    }

    public function getAverageBytesIn(): int {
        return array_sum(array_map(fn(array $data) => $data[1], $this->byteHistoryIn));
    }

    public function getTotalBytesOut(): int {
        return $this->totalBytesOut;
    }

    public function getTotalBytes(): int {
        return $this->totalBytesOut + $this->totalBytesIn;
    }

    public function getAverageBytesOut(): int {
        return array_sum(array_map(fn(array $data) => $data[1], $this->byteHistoryOut));
    }

    public function getAverageTotalBytes(): int {
        return $this->getAverageBytesOut() + $this->getAverageBytesIn();
    }

    public function getMonitorType(): string {
        return $this->monitorType;
    }
}