<?php

namespace pocketcloud\config;

use pocketcloud\lib\config\Configuration;
use pocketcloud\utils\SingletonTrait;
use pocketcloud\utils\Utils;

class CloudConfig extends Configuration {
    use SingletonTrait;

    /** @ignored */
    private string $generatedKey;
    private int $cloudPort = 3656;
    private bool $debugMode = true;
    private array $restAPI = [
        "enabled" => true,
        "port" => 8000,
        "auth-key" => "123"
    ];

    public function __construct() {
        self::setInstance($this);
        parent::__construct(STORAGE_PATH . "config.json", self::TYPE_JSON);
        $this->restAPI["auth-key"] = ($this->generatedKey = Utils::generateString(10));

        if (!$this->load()) $this->save();
    }

    public function reload(): void {
        $this->cloudPort = 3656;
        $this->debugMode = false;
        $this->restAPI = ["enabled" => true, "port" => 8000, "auth-key" => $this->generatedKey];
        $this->load();
    }

    public function getCloudPort(): int {
        return $this->cloudPort;
    }

    public function isDebugMode(): bool {
        return $this->debugMode;
    }

    public function isRestAPIEnabled(): bool {
        return $this->restAPI["enabled"];
    }

    public function getRestAPIPort(): int {
        return $this->restAPI["port"];
    }

    public function getRestAPIAuthKey(): string {
        return $this->restAPI["auth-key"];
    }
}