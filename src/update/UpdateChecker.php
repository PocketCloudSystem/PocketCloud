<?php

namespace pocketcloud\cloud\update;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\update\def\CloudPluginsUpdateChecker;
use pocketcloud\cloud\update\def\CloudUpdateChecker;
use pocketcloud\cloud\update\def\SoftwareUpdateChecker;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class UpdateChecker {
    use SingletonTrait;

    public const string TYPE_CLOUD = "cloud";
    public const string TYPE_LIBRARIES = "libraries";
    public const string TYPE_SERVER_SOFTWARE = "server_software";
    public const string TYPE_CLOUD_PLUGINS = "cloud_plugins";

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
        CloudLogger::get()->info("Checking for general updates...");
        foreach ($this->updateCheckers as $updateChecker) {
            if (!MainConfig::getInstance()->canCheckForUpdates($updateChecker->id())) {
                Server::getInstance()->addStartNotification("Skipped updates for §b{}§8, §ras it is disabled in the config.", CloudLogLevel::DEBUG(), $updateChecker->id());
                continue;
            }

            $updateChecker->needsUpdate()->then(function (array $result) use ($updateChecker): void {
                [$needsUpdate] = ($result = array_values($result));
                if (!$needsUpdate) return;
                if (!MainConfig::getInstance()->canUpdate($updateChecker->id())) {
                    CloudLogger::get()->debug("Skipping execution of updates for {}, as it is disabled in the config", $updateChecker::class);
                    return;
                }

                $updateChecker->update($result[1] ?? null)->then(function (?bool $success) use ($updateChecker): void {
                    if ($success === false) {
                        Server::getInstance()->shutdown();
                    }
                })->failure(fn() => Server::getInstance()->shutdown());
            });
        }
    }

    public function getUpdateCheckers(): array {
        return $this->updateCheckers;
    }
}