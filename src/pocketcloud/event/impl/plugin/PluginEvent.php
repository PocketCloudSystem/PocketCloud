<?php

namespace pocketcloud\event\impl\plugin;

use pocketcloud\event\Event;
use pocketcloud\plugin\CloudPlugin;

abstract class PluginEvent extends Event {

    public function __construct(private CloudPlugin $plugin) {}

    public function getPlugin(): CloudPlugin {
        return $this->plugin;
    }
}