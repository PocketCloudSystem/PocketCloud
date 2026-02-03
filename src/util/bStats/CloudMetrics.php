<?php

namespace pocketcloud\cloud\util\bStats;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\VersionInfo;
use Ramsey\Uuid\UuidInterface;
use xxFLORII\bStats\chart\def\SimplePieChart;
use xxFLORII\bStats\chart\def\SingleLineChart;
use xxFLORII\bStats\Metrics;
use xxFLORII\bStats\settings\MetricsSettings;

final readonly class CloudMetrics implements Tickable {

    private Metrics $metrics;

    public function __construct(
        private UuidInterface $uuid,
        private MainConfig $mainConfig
    ) {
        [$enabled, $logFailedRequests, $logSentData, $lobStatusResponseText] = array_values($this->mainConfig->getBStats());
        $this->metrics = new Metrics(new MetricsSettings(
            $enabled, VersionInfo::METRICS_ID,
            $logFailedRequests, $logSentData,
            $lobStatusResponseText, $this->uuid->toString()
        ));

        $this->metrics->addChart(new SingleLineChart("players", fn(): int => count(CloudPlayerManager::getInstance()->getAll())));
        $this->metrics->addChart(new SingleLineChart("managed_servers", fn(): int => count(CloudServerManager::getInstance()->getAll())));
        $this->metrics->addChart(new SimplePieChart("version", fn(): string => VersionInfo::VERSION));
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