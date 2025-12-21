<?php

namespace pocketcloud\cloud\util\benchmark;

use pocketcloud\cloud\util\FormatUtils;

final readonly class BenchmarkResult {

    public function __construct(
        private ?string $name,
        private int $iterations,
        private float $avg,
        private float $min,
        private float $max
    ) {}

    public function format(int $precision = 3): string {
        return sprintf(
            "Name: %s | Count: %s | Avg: %s | Min: %s | Max: %s",
            $this->name ?? "N/A",
            $this->iterations,
            FormatUtils::milliseconds($this->avg, $precision),
            FormatUtils::milliseconds($this->min, $precision),
            FormatUtils::milliseconds($this->max, $precision),
        );
    }

    public function getName(): ?string {
        return $this->name;
    }

    public function getIterations(): int {
        return $this->iterations;
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

    public function write(): array {
        return [
            "name" => $this->name,
            "iterations" => $this->iterations,
            "avg" => $this->avg,
            "min" => $this->min,
            "max" => $this->max
        ];
    }
}