<?php

namespace pocketcloud\cloud\update;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\update\def\CloudPluginsUpdateChecker;
use pocketcloud\cloud\update\def\CloudUpdateChecker;
use pocketcloud\cloud\update\def\SoftwareUpdateChecker;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class UpdateChecker {
    use SingletonTrait;

    /** @var array<IUpdateChecker> */
    private array $updateCheckers = [];

    public function __construct() {
        self::setInstance($this);
        $this->register(new CloudPluginsUpdateChecker());
        $this->register(new CloudUpdateChecker());
        $this->register(new SoftwareUpdateChecker());
    }

    public function register(IUpdateChecker $updateChecker): void {
        $this->updateCheckers[] = $updateChecker;
    }

    public function checkForUpdates(): void {
        if (!MainConfig::getInstance()->isUpdateChecks()) return;
        CloudLogger::get()->info("Checking for software updates...");
        foreach ($this->updateCheckers as $updateChecker) {
            $updateChecker->needsUpdate()->then(function (array $result) use ($updateChecker): void {
                [$needsUpdate] = ($result = array_values($result));
                if (!$needsUpdate) return;
                if (!MainConfig::getInstance()->isExecuteUpdates()) {
                    CloudLogger::get()->debug("Skipping updates for {}, as it is disabled in the config", $updateChecker::class);
                    return;
                }

                $updateChecker->update($result[1] ?? null)->then(function (bool $success) use ($updateChecker): void {
                    if (!$success) {
                        PocketCloud::getInstance()->shutdown();
                    }
                })->failure(fn() => PocketCloud::getInstance()->shutdown());
            });
        }
    }

    public function getUpdateCheckers(): array {
        return $this->updateCheckers;
    }
}