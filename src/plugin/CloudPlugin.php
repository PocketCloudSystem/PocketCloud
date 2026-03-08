<?php

namespace pocketcloud\cloud\plugin;

use pocketcloud\cloud\console\log\logger\PrefixedLogger;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\scheduler\TaskScheduler;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;

abstract class CloudPlugin {

    private bool $enabled = false;
    private TaskScheduler $scheduler;
    private PrefixedLogger $prefixedLogger;

    public function __construct(
        private readonly CloudPluginDescription $description,
        private readonly string $dataFolder,
        private readonly string $pluginDirPath
    ) {
        $this->scheduler = new TaskScheduler($this);
        $this->prefixedLogger = new PrefixedLogger($Server::getInstance()->logger, "[" . $this->description->getName() . "]");
    }

    public function saveResource(string $relativePath, bool $replace = false): bool {
        $absoluteSourcePath = PathUtils::join($this->pluginDirPath, "resources", $relativePath);
        $absoluteDestinationPath = PathUtils::join($this->dataFolder, $relativePath);
        if (!file_exists($absoluteSourcePath)) return false;
        if (file_exists($absoluteDestinationPath) && !$replace) return false;
        if (!file_exists(dirname($absoluteDestinationPath))) FileUtils::createDir(dirname($absoluteDestinationPath));
        return copy($absoluteSourcePath, $absoluteDestinationPath);
    }

    public function saveDefaultConfig(): bool {
        return $this->saveResource("config.yml");
    }

    public function onLoad(): void {}

    public function onEnable(): void {}

    public function onDisable(): void {}

    public function setEnabled(bool $enabled): void {
        $this->enabled = $enabled;
    }

    public function isEnabled(): bool {
        return $this->enabled;
    }

    public function isDisabled(): bool {
        return !$this->enabled;
    }

    public function getScheduler(): TaskScheduler {
        return $this->scheduler;
    }

    public function getLogger(): PrefixedLogger {
        return $this->prefixedLogger;
    }

    public function getDescription(): CloudPluginDescription {
        return $this->description;
    }

    public function getDataFolder(): string {
        return $this->dataFolder;
    }
}