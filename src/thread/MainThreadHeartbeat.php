<?php

namespace pocketcloud\cloud\thread;

use pmmp\thread\ThreadSafe;

final class MainThreadHeartbeat extends ThreadSafe {

    private float $lastBeat;
    private int $tick = 0;
    private string $stage = "boot";
    private bool $running = true;

    public function __construct() {
        $this->lastBeat = microtime(true);
    }

    public function beat(int $tick, string $stage): void {
        $this->synchronized(function () use ($tick, $stage): void {
            $this->lastBeat = microtime(true);
            $this->tick = $tick;
            $this->stage = $stage;
        });
    }

    public function stop(): void {
        $this->synchronized(function (): void {
            $this->running = false;
        });
    }

    public function getSnapshot(): array {
        return $this->synchronized(function (): array {
            return [
                "lastBeat" => $this->lastBeat,
                "tick" => $this->tick,
                "stage" => $this->stage,
                "running" => $this->running
            ];
        });
    }
}