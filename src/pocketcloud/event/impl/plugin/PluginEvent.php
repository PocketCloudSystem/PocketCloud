<?php

namespace pocketcloud\event\impl\plugin;

use pocketcloud\event\Event;
use pocketcloud\plugin\Plugin;

abstract class PluginEvent extends Event {

    public function __construct(private Plugin $plugin) {}

    public function getPlugin(): Plugin {
        return $this->plugin;
    }
}