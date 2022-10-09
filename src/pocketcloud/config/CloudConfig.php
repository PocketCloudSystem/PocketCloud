<?php

namespace pocketcloud\config;

use pocketcloud\utils\Config;
use pocketcloud\utils\SingletonTrait;

class CloudConfig {
    use SingletonTrait;

    private Config $config;
    private int $cloudPort;
    private bool $debugMode;

    public function __construct() {
        self::setInstance($this);
        $this->config = new Config(STORAGE_PATH . "config.json", 1);

        if (!$this->config->exists("cloud-port")) $this->config->set("cloud-port", mt_rand(100, 10000));
        if (!$this->config->exists("debug-mode")) $this->config->set("debug-mode", true);
        $this->config->save();

        $this->load();
    }

    private function load(): void {
        $this->cloudPort = $this->getConfig()->get("cloud-port");
        $this->debugMode = $this->getConfig()->get("debug-mode");
    }

    public function reload(): void {
        $this->config->reload();
        $this->load();
    }

    public function getCloudPort(): int {
        return $this->cloudPort;
    }

    public function isDebugMode(): bool {
        return $this->debugMode;
    }

    public function getConfig(): Config {
        return $this->config;
    }
}