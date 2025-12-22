<?php

namespace pocketcloud\cloud\config\impl;

use configlib\Configuration;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\util\trait\SingletonTrait;
use pocketcloud\cloud\util\Utils;
use Random\Randomizer;
use const pocketcloud\STORAGE_PATH;

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
    private array $binaries = [
        "server" => "https://github.com/pmmp/PHP-Binaries/releases/latest/download/PHP-{php_ver}-Linux-x86_64-PM5.tar.gz"
    ];

    private array $network = [
        "address" => "127.0.0.1",
        "port" => 3656,
        "encryption" => true,
        "only-local" => true
    ];

    private array $httpServer = [
        "enabled" => true,
        "address" => "127.0.0.1",
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

    private array $startCommands = [
        "server" => "%BINARY_PATH%bin/php7/bin/php %SOFTWARE_PATH%PocketMine-MP.phar --no-wizard",
        "proxy" => "java -jar %SOFTWARE_PATH%Waterdog.jar"
    ];

    private array $serverTimeouts = [
        "server" => 15,
        "proxy" => 20
    ];

    private array $serverPortRanges = [
        "server" => [
            "start" => 40000,
            "end" => 65535,
            "random-ports" => true
        ],
        "proxy" => [
            "start" => 19132,
            "end" => 20000,
            "random-ports" => false
        ]
    ];

    private int $serverPrepareThreads = 0; // By default, we are creating zero threads for that purpose to save some resources. Recommended to use if you've got more than 5 templates or 9 servers running at the same time

    public function __construct() {
        parent::__construct(STORAGE_PATH . "config.json", self::TYPE_JSON);
        self::setInstance($this);
        $this->httpServer["auth-key"] = ($this->generatedKey = Utils::generateString(10));

        $defaultBinaries = $this->binaries;
        $defaultNetwork = $this->network;
        $defaultHttp = $this->httpServer;
        $defaultMySql = $this->mysqlSettings;
        $defaultStartCommands = $this->startCommands;
        $defaultServerTimeouts = $this->serverTimeouts;
        $defaultServerPortRanges = $this->serverPortRanges;

        ExceptionHandler::tryCatch(function (array $defaultBinaries, array $defaultNetwork, array $defaultHttp, array $defaultMySql, array $defaultStartCommands, array $defaultServerTimeouts, array $defaultServerPortRanges): void {
            $this->load();
            foreach (array_keys($defaultBinaries) as $binary) {
                if (!isset($this->binaries[$binary])) $this->binaries[$binary] = str_replace(["{php_ver}"], [substr(PHP_VERSION, 0, 3)], $defaultBinaries[$binary]);
                else if ($this->binaries[$binary]) $this->binaries[$binary] = str_replace(["{php_ver}"], [substr(PHP_VERSION, 0, 3)], $this->binaries[$binary]);
            }

            Utils::fillMissingKeys($this->network, $defaultNetwork);
            Utils::fillMissingKeys($this->httpServer, $defaultHttp);
            Utils::fillMissingKeys($this->mysqlSettings, $defaultMySql);
            Utils::fillMissingKeys($this->startCommands, $defaultStartCommands);
            Utils::fillMissingKeys($this->serverTimeouts, $defaultServerTimeouts);
            Utils::fillMissingKeys($this->serverPortRanges, $defaultServerPortRanges);

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
                if (!isset($data["random-ports"])) $this->serverPortRanges[$key]["random-ports"] = (bool) round(new Randomizer()->getFloat(0, 1));

                $start = $this->serverPortRanges[$key]["start"];
                $end = $this->serverPortRanges[$key]["end"];
                $randomPorts = $this->serverPortRanges[$key]["random-ports"] ?? false;

                if ($start <= 0 || $end <= 0) {
                    PocketCloud::getInstance()->addStartNotification("Invalid port range §8(§b{$start}§8-§b{$end}§8) §rfor server type §8'§b" . $key . "§8'§r: §bStart §7or §bend §7can not be less or equal to §b0§r: §cResetting the entry, please review your config...", CloudLogLevel::WARN());
                    unset($this->serverPortRanges[$key]);
                    continue;
                }

                if ($start > $end) {
                    PocketCloud::getInstance()->addStartNotification("Invalid port range §8(§b{$start}§8-§b{$end}§8) §rfor server type §8'§b" . $key . "§8'§r: §bStart §ris §chigher §rthan §bend§r: §cResetting the entry, please review your config...", CloudLogLevel::WARN());
                    unset($this->serverPortRanges[$key]);
                    continue;
                }

                if (($start + 50) > $end) {
                    PocketCloud::getInstance()->addStartNotification("Invalid port range §8(§b{$start}§8-§b{$end}§8) §rfor server type §8'§b" . $key . "§8'§r: §bEnd §rneeds to be at least §b50 ports higher §rthan §bstart§r: §cResetting the entry, please review your config...", CloudLogLevel::WARN());
                    unset($this->serverPortRanges[$key]);
                }

                // Re-setting this due to strict declarations, see ServerUtils
                $this->serverPortRanges[$key] = [
                    "start" => $start, "end" => $end, "random-ports" => $randomPorts
                ];
            }

            $this->save();
        }, "Failed to load main config", fn() => PocketCloud::getInstance()->shutdown(), $defaultBinaries, $defaultNetwork, $defaultHttp, $defaultMySql, $defaultStartCommands, $defaultServerTimeouts, $defaultServerPortRanges);
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

    public function setBinaries(string $templateType, string $url): void {
        $this->binaries[$templateType] = $url;
    }

    public function setNetworkAddress(string $address): void {
        $this->network["address"] = $address;
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

    public function setHttpServerAddress(string $value): void {
        $this->httpServer["address"] = $value;
    }

    public function setHttpServerPort(int $value): void {
        $this->httpServer["port"] = $value;
    }

    public function setHttpServerOnlyLocal(bool $value): void {
        $this->httpServer["onlyLocal"] = $value;
    }

    public function setStartCommand(string $templateType, string $startCommand): void {
        $this->startCommands[strtolower($templateType)] = $startCommand;
    }

    public function setServerTimeouts(string $templateType, int $timeout): void {
        $this->serverTimeouts[strtolower($templateType)] = $timeout;
    }

    public function setServerPortRange(string $templateType, int $start, int $end, bool $randomPorts): void {
        $this->serverPortRanges[strtolower($templateType)] = ["random-ports" => $randomPorts, "start" => $start, "end" => $end];
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

    public function getBinaries(string $templateType): ?string {
        return $this->binaries[$templateType] ?? null;
    }

    public function getAllBinaries(): array {
        return $this->binaries;
    }

    public function getNetwork(): array {
        return $this->network;
    }

    public function getNetworkAddress(): int {
        return $this->network["address"];
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

    public function getHttpServerAddress(): int {
        return $this->httpServer["address"];
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

    public function getStartCommand(string $software): string {
        return $this->startCommands[strtolower($software)] ?? "";
    }

    public function getStartCommands(): array {
        return $this->startCommands;
    }

    public function getServerTimeout(string $templateType): int {
        return $this->serverTimeouts[strtolower($templateType)]; #?? ServerUtils::DEFAULT_TIMEOUT; //TODO
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