<?php

namespace pocketcloud\cloud\event\impl\plugin;

use pocketcloud\cloud\event\Event;
use pocketcloud\cloud\plugin\CloudPlugin;

abstract class PluginEvent extends Event {

    public function __construct(private readonly CloudPlugin $plugin) {}

    public function getPlugin(): CloudPlugin {
        return $this->plugin;
    }
}