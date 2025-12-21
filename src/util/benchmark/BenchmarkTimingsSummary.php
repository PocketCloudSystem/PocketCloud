<?php

namespace pocketcloud\cloud\util\benchmark;

use pocketcloud\cloud\util\FormatUtils;

final readonly class BenchmarkTimingsSummary {

    public function __construct(
        private string $name,
        private int $count,
        private float $avg,
        private float $min,
        private float $max
    ) {}

    public function format(int $precision = 3): string {
        return sprintf(
            "Name: %s | Count: %s | Avg: %s | Min: %s | Max: %s",
            $this->name ?? "N/A",
            $this->count,
            FormatUtils::milliseconds($this->avg, $precision),
            FormatUtils::milliseconds($this->min, $precision),
            FormatUtils::milliseconds($this->max, $precision),
        );
    }

    public function getName(): string {
        return $this->name;
    }

    public function getCount(): int {
        return $this->count;
    }

    public function getAvg(): float {
        return $this->avg;
    }

    public function getMin(): float {
        return $this->min;
    }

    public function getMax(): float {
        return $this->max;
    }
}