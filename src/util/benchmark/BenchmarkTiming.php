<?php

namespace pocketcloud\cloud\util\benchmark;

final class BenchmarkTiming {

    private bool $running = false;
    private ?float $start = null;
    private ?float $end = null;
    private ?float $duration = null;

    public function __construct(
        private readonly string $name,
        private readonly int $currentTick
    ) {}

    public function startTiming(): void {
        if ($this->running) return;
        $this->running = true;
        $this->start = hrtime(true);
    }

    public function stopTiming(): void {
        if (!$this->running) return;
        $this->running = false;
        $this->end = hrtime(true);
        $this->duration = ($this->end - $this->start) / 1_000_000;
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

    public function getStartInMs(): ?float {
        if ($this->start === null) return null;
        return $this->start / 1_000_000;
    }

    public function getEnd(): ?float {
        return $this->end;
    }

    public function getEndInMs(): ?float {
        if ($this->end === null) return null;
        return $this->end / 1_000_000;
    }

    public function getDuration(): ?float {
        return $this->duration;
    }

    public function getName(): string {
        return $this->name;
    }

    public function getCurrentTick(): int {
        return $this->currentTick;
    }
}