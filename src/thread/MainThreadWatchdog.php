<?php

namespace pocketcloud\cloud\thread;

use Throwable;
use const pocketcloud\STORAGE_PATH;

final class MainThreadWatchdog extends Thread {

    private const float CHECK_INTERVAL_SECONDS = 0.5;
    private const float FREEZE_THRESHOLD_SECONDS = 3.0;
    private const float REPORT_INTERVAL_SECONDS = 10.0;

    private float $lastReportTime = 0.0;

    public function __construct(
        private readonly MainThreadHeartbeat $heartbeat
    ) {}

    protected function onRun(): void {
        while ($this->isAlive()) {
            $snapshot = $this->heartbeat->getSnapshot();
            if (!$snapshot["running"]) break;

            $now = microtime(true);
            $stalledFor = $now - $snapshot["lastBeat"];
            if (
                $stalledFor >= self::FREEZE_THRESHOLD_SECONDS &&
                ($now - $this->lastReportTime) >= self::REPORT_INTERVAL_SECONDS
            ) {
                $this->lastReportTime = $now;
                $this->writeFreezeReport($snapshot, $stalledFor);
            }

            usleep((int) (self::CHECK_INTERVAL_SECONDS * 1_000_000));
        }
    }

    private function writeFreezeReport(array $snapshot, float $stalledFor): void {
        $line = sprintf(
            "[%s] Main thread heartbeat stalled for %.3fs at tick %d, stage: %s%s",
            date("Y-m-d H:i:s"),
            $stalledFor,
            $snapshot["tick"],
            $snapshot["stage"],
            PHP_EOL
        );

        try {
            echo $line . "\n";
            file_put_contents(STORAGE_PATH . "watchdog.log", $line, FILE_APPEND | LOCK_EX);
        } catch (Throwable) {}
    }
}
