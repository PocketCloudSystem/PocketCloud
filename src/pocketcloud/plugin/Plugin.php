<?php

namespace pocketcloud\plugin;

abstract class Plugin {

    public function __construct(private PluginDescription $description) {}

    private bool $enabled = false;

    public function onLoad(): void {}

    public function onEnable(): void {}

    public function onDisable(): void {}

    public function setEnabled(bool $enabled) {
        $this->enabled = $enabled;
    }

    public function getDescription(): PluginDescription {
        return $this->description;
    }

    public function isEnabled(): bool {
        return $this->enabled;
    }

    public function isDisabled(): bool {
        return !$this->enabled;
    }
}