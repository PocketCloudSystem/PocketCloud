<?php

namespace pocketcloud\cloud\traffic;

use Closure;

abstract class TrafficMonitor {

    public const string REGULAR_MODE_IN = "in";
    public const string REGULAR_MODE_OUT = "out";

    protected bool $active = true;
    protected int $timestamp;
    protected ?int $monitoringDuration = null;

    /** @var array<array<Closure>> */
    protected array $handlers = [];

    protected int $totalBytesIn = 0;
    protected int $totalBytesOut = 0;

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
        switch (strtolower($mode)) {
            case self::REGULAR_MODE_IN: {
                $this->totalBytesIn += $bytes;
                break;
            }
            case self::REGULAR_MODE_OUT: {
                $this->totalBytesOut += $bytes;
                break;
            }
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

    final public function stopMonitoring(): void {
        if (!$this->active) return;
        $this->active = false;
        $this->handlers = [];
        $this->monitoringDuration = time() - $this->timestamp;
        TrafficMonitorManager::getInstance()->removeTrafficMonitor($this);
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

    public function getTotalBytesOut(): int {
        return $this->totalBytesOut;
    }

    public function getMonitorType(): string {
        return $this->monitorType;
    }
}