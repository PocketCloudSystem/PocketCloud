<?php

namespace pocketcloud\cloud\util\bStats;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\VersionInfo;
use Ramsey\Uuid\UuidInterface;
use xxFLORII\bStats\Metrics;
use xxFLORII\bStats\settings\MetricsSettings;

final class CloudMetrics implements Tickable {

    private readonly Metrics $metrics;

    public function __construct(
        private readonly UuidInterface $uuid,
        private readonly MainConfig $mainConfig
    ) {
        $this->metrics = new Metrics(new MetricsSettings(
            $this->mainConfig->isBStatsEnabled(), VersionInfo::METRICS_ID,
            $this->mainConfig->isBStatsLogFailedRequests(), $this->mainConfig->isBStatsLogSentData(),
            $this->mainConfig->isBStatsLogStatusResponseText(), $this->uuid->toString()
        ));
    }

    public function tick(int $currentTick): void {
        if (!$this->metrics->getSettings()->isEnabled()) return;
        $this->metrics->tick($currentTick);
    }

    public function getMetrics(): Metrics {
        return $this->metrics;
    }

    public function getUuid(): UuidInterface {
        return $this->uuid;
    }

    public function getMainConfig(): MainConfig {
        return $this->mainConfig;
    }
}