<?php

namespace pocketcloud\cloud\config\impl;

use configlib\Configuration;
use pocketcloud\cloud\exception\ExceptionHandler;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\server\util\ServerUtils;
use pocketcloud\cloud\terminal\log\level\CloudLogLevel;
use pocketcloud\cloud\util\SingletonTrait;
use pocketcloud\cloud\util\Utils;

final class MainConfig extends Configuration {
    use SingletonTrait;

    /** @ignored */
    private string $generatedKey;
    private int $memoryLimit = 512;
    private string $language = "en_US";
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

    private array $serverTimeouts = [
        "server" => 15,
        "proxy" => 20
    ];

    private array $serverPortRanges = [
        "server" => [
            "start" => 40000,
            "end" => 65535
        ],
        "proxy" => [
            "start" => 19132,
            "end" => 20000
        ]
    ];

    private int $serverPrepareThreads = 0; // By default, we are creating zero threads for that purpose to save some resources. Recommended to use if you've got more than 5 templates or 9 servers running at the same time

    public function __construct() {
        parent::__construct(STORAGE_PATH . "config.json", self::TYPE_JSON);
        self::setInstance($this);
        $this->httpServer["auth-key"] = ($this->generatedKey = Utils::generateString(10));

        $defaultHttp = $this->httpServer;
        $defaultNetwork = $this->network;
        $defaultWeb = $this->web;
        $defaultMySql = $this->mysqlSettings;
        $defaultStartCommands = $this->startCommands;
        $defaultServerTimeouts = $this->serverTimeouts;
        $defaultServerPortRanges = $this->serverPortRanges;

        ExceptionHandler::tryCatch(function (array $defaultHttp, array $defaultNetwork, array $defaultWeb, array $defaultMySql, array $defaultStartCommands, array $defaultServerTimeouts, array $defaultServerPortRanges): void {
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

            foreach (array_keys($defaultStartCommands) as $key) {
                if (!isset($this->startCommands[$key])) $this->startCommands[$key] = $defaultStartCommands[$key];
            }

            foreach (array_keys($defaultServerTimeouts) as $key) {
                if (!isset($this->serverTimeouts[$key])) $this->serverTimeouts[$key] = $defaultServerTimeouts[$key];
            }

            if (!in_array(strtolower($this->startMethod), ["tmux", "screen"])) {
                $this->startMethod = "tmux";
            }

            if (!in_array(strtolower($this->provider), ["mysql", "json"])) {
                $this->provider = "json";
            }

            if ($this->serverPrepareThreads < 0) $this->serverPrepareThreads = 0; // If this is 0, server preparing remains inside the main-thread, therefore blocking it during the process
            else if ($this->serverPrepareThreads > 5) $this->serverPrepareThreads = 5;

            foreach ($this->serverPortRanges as $key => $data) {
                if (!is_array($data)) $this->serverPortRanges[$key] = [];
                if (!isset($data["start"])) $this->serverPortRanges[$key]["start"] = mt_rand(40000, 41000);
                if (!isset($data["end"])) $this->serverPortRanges[$key]["end"] = mt_rand(41000, 42000);

                $start = $this->serverPortRanges[$key]["start"];
                $end = $this->serverPortRanges[$key]["end"];

                if ($start <= 0 || $end <= 0) {
                    PocketCloud::getInstance()->notifyOnStart("Invalid port range §8(§b{$start}§8-§b{$end}§8) §rfor server type §8'§b" . $key . "§8'§r: §bStart §7or §bend §7can not be less or equal to §b0§r: §cResetting the entry, please review your config...", CloudLogLevel::WARN());
                    unset($this->serverPortRanges[$key]);
                    continue;
                }

                if ($start > $end) {
                    PocketCloud::getInstance()->notifyOnStart("Invalid port range §8(§b{$start}§8-§b{$end}§8) §rfor server type §8'§b" . $key . "§8'§r: §bStart §ris §chigher §rthan §bend§r: §cResetting the entry, please review your config...", CloudLogLevel::WARN());
                    unset($this->serverPortRanges[$key]);
                    continue;
                }

                if (($start + 50) > $end) {
                    PocketCloud::getInstance()->notifyOnStart("Invalid port range §8(§b{$start}§8-§b{$end}§8) §rfor server type §8'§b" . $key . "§8'§r: §bEnd §rneeds to be at least §b50 ports higher §rthan §bstart§r: §cResetting the entry, please review your config...", CloudLogLevel::WARN());
                    unset($this->serverPortRanges[$key]);
                }
            }

            foreach (array_keys($defaultServerPortRanges) as $key) {
                if (!isset($this->serverPortRanges[$key])) $this->serverPortRanges[$key] = $defaultServerPortRanges[$key];
            }

            $this->save();
        }, "Failed to load main config", fn() => PocketCloud::getInstance()->shutdown(), $defaultHttp, $defaultNetwork, $defaultWeb, $defaultMySql, $defaultStartCommands, $defaultServerTimeouts, $defaultServerPortRanges);
    }

    public function setMemoryLimit(int $memoryLimit): void {
        $this->memoryLimit = $memoryLimit;
        ini_set("memory_limit", ($memoryLimit <= 0 ? "-1" : $memoryLimit . "M"));
    }

    public function setLanguage(string $language): void {
        $this->language = $language;
    }

    public function setProvider(string $provider): void {
        $this->provider = $provider;
        CloudProvider::select();
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

    public function setStartCommand(string $templateType, string $startCommand): void {
        $this->startCommands[strtolower($templateType)] = $startCommand;
    }

    public function setServerTimeouts(string $templateType, int $timeout): void {
        $this->serverTimeouts[strtolower($templateType)] = $timeout;
    }

    public function setServerPortRange(string $templateType, int $start, int $end): void {
        $this->serverPortRanges[strtolower($templateType)] = ["start" => $start, "end" => $end];
    }

    public function setServerPrepareThreads(int $serverPrepareThreads): void {
        if ($serverPrepareThreads < 0) $serverPrepareThreads = 0;
        else if ($serverPrepareThreads > 5) $serverPrepareThreads = 5;
        $this->serverPrepareThreads = $serverPrepareThreads;
    }

    public function getMemoryLimit(): int {
        return $this->memoryLimit;
    }

    public function getLanguage(): string {
        return $this->language;
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

    public function getServerTimeout(string $templateType): int {
        return $this->serverTimeouts[strtolower($templateType)] ?? ServerUtils::DEFAULT_TIMEOUT;
    }

    public function getServerTimeouts(): array {
        return $this->serverTimeouts;
    }

    public function getServerPortRange(string $templateType): ?array {
        return $this->serverPortRanges[strtolower($templateType)] ?? null;
    }

    public function getServerPortRanges(): array {
        return $this->serverPortRanges;
    }

    public function getServerPrepareThreads(): int {
        return $this->serverPrepareThreads;
    }
}