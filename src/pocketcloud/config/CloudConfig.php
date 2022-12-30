<?php

namespace pocketcloud\config;

use pocketcloud\utils\Config;
use pocketcloud\utils\SingletonTrait;
use pocketcloud\utils\Utils;

class CloudConfig {
    use SingletonTrait;

    private Config $config;
    private int $cloudPort;
    private bool $debugMode;
    private bool $restApiEnabled;
    private int $restApiPort;
    private string $restApiAuthKey;

    public function __construct() {
        self::setInstance($this);
        $this->config = new Config(STORAGE_PATH . "config.json", 1);

        if (!$this->config->exists("cloud-port")) $this->config->set("cloud-port", mt_rand(100, 10000));
        if (!$this->config->exists("debug-mode")) $this->config->set("debug-mode", true);
        if (!$this->config->exists("rest-api")) $this->config->set("rest-api", ["enabled" => true, "port" => 8000, "auth-key" => Utils::generateString(10)]);
        $this->config->save();

        $this->load();
    }

    private function load(): void {
        $this->cloudPort = $this->getConfig()->get("cloud-port");
        $this->debugMode = $this->getConfig()->get("debug-mode");
        $this->restApiEnabled = $this->getConfig()->get("rest-api")["enabled"];
        $this->restApiPort = $this->getConfig()->get("rest-api")["port"];
        $this->restApiAuthKey = $this->getConfig()->get("rest-api")["auth-key"];
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

    public function isRestAPIEnabled(): bool {
        return $this->restApiEnabled;
    }

    public function getRestAPIPort(): int {
        return $this->restApiPort;
    }

    public function getRestAPIAuthKey(): string {
        return $this->restApiAuthKey;
    }

    public function getConfig(): Config {
        return $this->config;
    }
}