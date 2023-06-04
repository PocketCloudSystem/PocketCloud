<?php

namespace pocketcloud\config;

use configlib\Configuration;
use pocketcloud\util\Reloadable;
use pocketcloud\util\SingletonTrait;
use pocketcloud\util\Utils;

class DefaultConfig extends Configuration implements Reloadable {
    use SingletonTrait;

    /** @ignored */
    private string $generatedKey;
    private string $language = "en_US";
    private int $memoryLimit = 512;
    private bool $debugMode = false;
    private string $startMethod = "tmux";
    private array $network = [
        "port" => 3656,
        "encryption" => true
    ];
    private array $httpServer = [
        "enabled" => true,
        "port" => 8000,
        "auth-key" => "123"
    ];
    private array $startCommands = [
        "server" => "%CLOUD_PATH%bin/php7/bin/php %SOFTWARE_PATH%PocketMine-MP.phar --no-wizard",
        "proxy" => "java -jar %SOFTWARE_PATH%Waterdog.jar"
    ];

    public function __construct() {
        self::setInstance($this);
        parent::__construct(STORAGE_PATH . "config.json", self::TYPE_JSON);
        $this->httpServer["auth-key"] = ($this->generatedKey = Utils::generateString(10));

        if (!$this->load()) $this->save();
    }

    public function reload(): bool {
        $this->language = "en_US";
        $this->memoryLimit = 512;
        $this->debugMode = false;
        $this->startMethod = "tmux";
        $this->network = ["port" => 3656, "encryption" => true];
        $this->httpServer = ["enabled" => true, "port" => 8000, "auth-key" => $this->generatedKey];
        $this->startCommands = ["server" => "bin/php/bin/php %SOFTWARE_PATH%PocketMine-MP.phar", "proxy" => "java -jar %SOFTWARE_PATH%Waterdog.jar"];
        return $this->load();
    }

    public function setLanguage(string $language): void {
        $this->language = $language;
    }

    public function setMemoryLimit(int $memoryLimit): void {
        $this->memoryLimit = $memoryLimit;
        ini_set("memory_limit", ($memoryLimit < 0 ? "-1" : $memoryLimit . "M"));
    }

    public function setDebugMode(bool $debugMode): void {
        $this->debugMode = $debugMode;
    }

    public function setStartMethod(string $startMethod): void {
        $this->startMethod = $startMethod;
    }

    public function setNetworkPort(int $port): void {
        $this->network["port"] = $port;
    }

    public function setNetworkEncryption(bool $value): void {
        $this->network["encryption"] = $value;
    }

    public function setHttpServerEnabled(bool $value): void {
        $this->httpServer["enabled"] = $value;
    }

    public function setHttpServerPort(int $value): void {
        $this->httpServer["port"] = $value;
    }

    public function getLanguage(): string {
        return $this->language;
    }

    public function getMemoryLimit(): int {
        return $this->memoryLimit;
    }

    public function isDebugMode(): bool {
        return $this->debugMode;
    }

    public function getStartMethod(): string {
        return $this->startMethod;
    }

    public function getNetworkPort(): int {
        return $this->network["port"];
    }

    public function isNetworkEncryptionEnabled(): bool {
        return $this->network["encryption"];
    }

    public function isHttpServerEnabled(): bool {
        return $this->httpServer["enabled"];
    }

    public function getHttpServerPort(): int {
        return $this->httpServer["port"];
    }

    public function getHttpServerAuthKey(): string {
        return $this->httpServer["auth-key"];
    }

    public function getStartCommand(string $software): string {
        return $this->startCommands[strtolower($software)] ?? "";
    }

    public function getStartCommands(): array {
        return $this->startCommands;
    }
}