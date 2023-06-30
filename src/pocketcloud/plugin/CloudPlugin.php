<?php

namespace pocketcloud\plugin;

use pocketcloud\scheduler\TaskScheduler;

abstract class CloudPlugin {

    private bool $enabled = false;
    private TaskScheduler $scheduler;

    public function __construct(private readonly CloudPluginDescription $description) {
        $this->scheduler = new TaskScheduler($this);
    }

    public function onLoad(): void {}

    public function onEnable(): void {}

    public function onDisable(): void {}

    public function setEnabled(bool $enabled): void {
        $this->enabled = $enabled;
    }

    public function getDescription(): CloudPluginDescription {
        return $this->description;
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
}