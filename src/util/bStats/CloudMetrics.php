<?php

namespace pocketcloud\cloud\util\bStats;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\VersionInfo;
use Ramsey\Uuid\UuidInterface;
use xxFLORII\bStats\Metrics;
use xxFLORII\bStats\settings\MetricsSettings;

final class CloudMetrics implements Tickable {

    public const int DEFAULT_SUBMIT_INTERVAL = 20 * 60 * 30;

    private readonly Metrics $metrics;
    private int $nextSubmitTick = 0;

    public function __construct(
        private readonly UuidInterface $uuid,
        private readonly MainConfig $mainConfig
    ) {
        $this->metrics = new Metrics(new MetricsSettings(
            false, VersionInfo::METRICS_ID,
            $this->mainConfig->isBStatsLogFailedRequests(), $this->mainConfig->isBStatsLogSentData(),
            $this->mainConfig->isBStatsLogStatusResponseText(), $this->uuid->toString()
        ));
    }

    public function tick(int $currentTick): void {
        if (!$this->metrics->getMetricsSettings()->isEnabled()) return;
        if ($currentTick >= $this->nextSubmitTick) {
            $this->nextSubmitTick = $currentTick + self::DEFAULT_SUBMIT_INTERVAL;
            CloudLogger::get()->debug("Sending bStats data...");
            $this->metrics->sendData()->then(function (mixed $result): void {
                CloudLogger::get()->debug("Successfully submitted bStats data");
                if ($this->metrics->getMetricsSettings()->isLogResponseStatusText()) CloudLogger::get()->forceDebug("bStats response:" . $result);
            })->failure(function(array|string $errorInfo): void {
                if ($this->metrics->getMetricsSettings()->isLogFailedRequests()) {
                    if (is_array($errorInfo)) {
                        [$response, $error, $status] = $errorInfo;
                        CloudLogger::get()->error("Failed to submit data to bStats §8(§cHTTP Status Code §e{}§8)§c: §e{}", $status, ($error == "" ? $response : $error));
                    } else {
                        CloudLogger::get()->error("Failed to submit data to bStats: §e{}", $errorInfo);
                    }
                }
            });
        }
    }

    public function getMetrics(): Metrics {
        return $this->metrics;
    }

    public function getNextSubmitTick(): int {
        return $this->nextSubmitTick;
    }

    public function getUuid(): UuidInterface {
        return $this->uuid;
    }

    public function getMainConfig(): MainConfig {
        return $this->mainConfig;
    }
}