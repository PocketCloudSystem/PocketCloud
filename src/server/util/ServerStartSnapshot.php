<?php

namespace pocketcloud\cloud\server\util;

use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\ProcessUtils;

final readonly class ServerStartSnapshot {

    public function __construct(
        public float $tps,
        public float $avgTps,
        public float $tickUsage,
        public float $cpuUsage,
        public float $memoryUsage,
        public float $capturedAt
    ) {}

    public function getTps(): float {
        return $this->tps;
    }

    public function getAvgTps(): float {
        return $this->avgTps;
    }

    public function getTickUsage(): float {
        return $this->tickUsage;
    }

    public function getCpuUsage(): float {
        return $this->cpuUsage;
    }

    public function getMemoryUsage(): float {
        return $this->memoryUsage;
    }

    public function getCapturedAt(): float {
        return $this->capturedAt;
    }

    public static function capture(): self {
        $cloud = PocketCloud::getInstance();
        $status = ProcessUtils::getProcessStatus();
        return new self(
            $cloud->getCurrentTPS(),
            $cloud->getAverageTPS(),
            $cloud->getTickUsage(),
            ProcessUtils::getCpuUsage(),
            ($status["rss"] ?? 0) / 1024 / 1024,
            microtime(true)
        );
    }
}