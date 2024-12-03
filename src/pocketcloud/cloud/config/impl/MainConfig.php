<?php

namespace pocketcloud\cloud\config\impl;

use configlib\Configuration;
use pocketcloud\cloud\exception\ExceptionHandler;
use pocketcloud\cloud\util\SingletonTrait;
use pocketcloud\cloud\util\Utils;

final class MainConfig extends Configuration {
    use SingletonTrait;

    /** @ignored */
    private string $generatedKey;
    private int $memoryLimit = 512;
    private string $provider = "json";
    private bool $debugMode = false;
    private bool $updateChecks = true;
    private bool $executeUpdates = true;
    private string $startMethod = "tmux";
    private array $network = [
        "port" => 3656,
        "encryption" => true,
        "only-local" => true
    ];

    private array $httpServer = [
        "enabled" => true,
        "port" => 8000,
        "auth-key" => "123",
        "only-local" => true
    ];

    private array $mysqlSettings = [
        "address" => "127.0.0.1",
        "port" => 3306,
        "user" => "root",
        "password" => "pastepasswordinhere",
        "database" => "cloud"
    ];

    private array $web = [
        "enabled" => false
    ];

    private array $startCommands = [
        "server" => "%CLOUD_PATH%bin/php7/bin/php %SOFTWARE_PATH%PocketMine-MP.phar --no-wizard",
        "proxy" => "java -jar %SOFTWARE_PATH%Waterdog.jar"
    ];

    public function __construct() {
        self::setInstance($this);
        parent::__construct(STORAGE_PATH . "config.json", self::TYPE_JSON);
        $this->httpServer["auth-key"] = ($this->generatedKey = Utils::generateString(10));

        $defaultHttp = $this->httpServer;
        $defaultNetwork = $this->network;
        $defaultWeb = $this->web;
        $defaultMySql = $this->mysqlSettings;

        ExceptionHandler::tryCatch(function (array $defaultHttp, array $defaultNetwork, array $defaultWeb, array $defaultMySql): void {
            $this->load();
            foreach (array_keys($defaultHttp) as $key) {
                if (!isset($this->httpServer[$key])) $this->httpServer[$key] = $defaultHttp[$key];
            }

            foreach (array_keys($defaultNetwork) as $key) {
                if (!isset($this->network[$key])) $this->network[$key] = $defaultNetwork[$key];
            }

            foreach (array_keys($defaultWeb) as $key) {
                if (!isset($this->web[$key])) $this->web[$key] = $defaultWeb[$key];
            }

            foreach (array_keys($defaultMySql) as $key) {
                if (!isset($this->mysqlSettings[$key])) $this->mysqlSettings[$key] = $defaultMySql[$key];
            }

            if (!in_array(strtolower($this->provider), ["mysql", "json"])) {
                $this->provider = "json";
            }

            $this->save();
        }, "Failed to load main config", null, $defaultHttp, $defaultNetwork, $defaultWeb, $defaultMySql);
    }

    public function setMemoryLimit(int $memoryLimit): void {
        $this->memoryLimit = $memoryLimit;
        ini_set("memory_limit", ($memoryLimit < 0 ? "-1" : $memoryLimit . "M"));
    }

    public function setDebugMode(bool $debugMode): void {
        $this->debugMode = $debugMode;
    }

    public function setUpdateChecks(bool $updateChecks): void {
        $this->updateChecks = $updateChecks;
    }

    public function setExecuteUpdates(bool $executeUpdates): void {
        $this->executeUpdates = $executeUpdates;
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

    public function setNetworkOnlyLocal(bool $value): void {
        $this->network["onlyLocal"] = $value;
    }

    public function setHttpServerEnabled(bool $value): void {
        $this->httpServer["enabled"] = $value;
    }

    public function setHttpServerPort(int $value): void {
        $this->httpServer["port"] = $value;
    }

    public function setHttpServerOnlyLocal(bool $value): void {
        $this->httpServer["onlyLocal"] = $value;
    }

    public function setWebEnabled(bool $value): void {
        $this->web["enabled"] = $value;
    }

    public function getMemoryLimit(): int {
        return $this->memoryLimit;
    }

    public function getProvider(): string {
        return strtolower($this->provider);
    }

    public function isDebugMode(): bool {
        return $this->debugMode;
    }

    public function isUpdateChecks(): bool {
        return $this->updateChecks;
    }

    public function isExecuteUpdates(): bool {
        return $this->executeUpdates;
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

    public function isNetworkOnlyLocal(): bool {
        return $this->network["only-local"] ?? true;
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

    public function isHttpServerOnlyLocal(): bool {
        return $this->httpServer["only-local"] ?? true;
    }

    public function getMySqlAddress(): string {
        return $this->mysqlSettings["address"];
    }

    public function getMySqlPort(): int {
        return $this->mysqlSettings["port"];
    }

    public function getMySqlUser(): string {
        return $this->mysqlSettings["user"];
    }

    public function getMySqlPassword(): string {
        return $this->mysqlSettings["password"];
    }

    public function getMySqlDatabase(): string {
        return $this->mysqlSettings["database"];
    }

    public function isWebEnabled(): bool {
        return $this->web["enabled"];
    }

    public function getStartCommand(string $software): string {
        return $this->startCommands[strtolower($software)] ?? "";
    }

    public function getStartCommands(): array {
        return $this->startCommands;
    }
}