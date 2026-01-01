<?php

namespace pocketcloud\cloud\plugin;

use pocketcloud\cloud\console\log\logger\PrefixedLogger;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\scheduler\TaskScheduler;

abstract class CloudPlugin {

    private bool $enabled = false;
    private TaskScheduler $scheduler;
    private PrefixedLogger $prefixedLogger;

    public function __construct(private readonly CloudPluginDescription $description) {
        $this->scheduler = new TaskScheduler($this);
        $this->prefixedLogger = new PrefixedLogger(PocketCloud::getInstance()->getLogger(), "[" . $this->description->getName() . "]");
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
}