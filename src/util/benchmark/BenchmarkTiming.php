<?php

namespace pocketcloud\cloud\util\benchmark;

final class BenchmarkTiming {

    private bool $running = false;
    private ?float $start = null;
    private ?float $duration = null;

    public function __construct(private readonly string $name) {}

    public function startTiming(): void {
        if ($this->running) return;
        $this->running = true;
        $this->start = hrtime(true);
    }

    public function stopTiming(): void {
        if (!$this->running) return;
        $this->running = false;
        $end = hrtime(true);
        $this->duration = ($end - $this->start) / 1_000_000;
    }

    public function isRunning(): bool {
        return $this->running;
    }

    public function isDone(): bool {
        return $this->duration !== null;
    }

    public function getStart(): ?float {
        return $this->start;
    }

    public function getDuration(): ?float {
        return $this->duration;
    }

    public function getName(): string {
        return $this->name;
    }
}