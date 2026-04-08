<?php

namespace pocketcloud\cloud\config\impl;

use configlib\Configuration;
use InvalidArgumentException;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\update\IUpdateChecker;
use pocketcloud\cloud\util\trait\SingletonTrait;
use pocketcloud\cloud\util\Utils;
use const pocketcloud\STORAGE_PATH;

final class MainConfig extends Configuration {
    use SingletonTrait;

    /** @ignored */
    private string $generatedKey;
    private string $cloudName = "main-cloud";
    private int $memoryLimit = 512;
    private string $language = "en_US";
    private string $provider = "json";
    private array $individualUpdateChecks = [
        "cloud" => [
            "check" => true
        ],
        "cloud_plugins" => [
            "check" => true,
            "update" => false
        ],
        "libraries" => [
            "check" => true,
            "update" => false
        ],
        "server_software" => [
            "check" => true,
            "update" => false
        ]
    ];
    private bool $startUpDelay = true;
    private bool $writeTimingsOnShutdown = true;
    private array $bStats = [
        "enabled" => true,
        "log_failed_requests" => false,
        "log_sent_data" => false,
        "log_response_status_text" => false
    ];

    private array $network = [
        "address" => "127.0.0.1",
        "port" => 3656,
        "encryption" => true,
        "packet_size_limit" => 1024 * 1024 * 10
    ];

    private array $httpServer = [
        "enabled" => true,
        "address" => "127.0.0.1",
        "port" => 8000,
        "auth-key" => "123",
        "only-local" => true,
        "rate-limit" => [
            "enabled" => false,
            "timeout_in_seconds" => 120,
            "max_requests" => 20,
            "time_frame_in_seconds" => 60
        ],
        "response-caching" => [
            "enabled" => false,
            "caching_time_in_seconds" => 60
        ]
    ];

    private array $httpClient = [
        "thread-count" => 1
    ];

    private array $mysqlSettings = [
        "address" => "127.0.0.1",
        "port" => 3306,
        "user" => "root",
        "password" => "pastepasswordinhere",
        "database" => "cloud"
    ];

    public function __construct() {
        parent::__construct(STORAGE_PATH . "config.json", self::TYPE_JSON);
        self::setInstance($this);
        $this->httpServer["auth-key"] = ($this->generatedKey = Utils::generateString(10));

        $defaultUpdateChecks = $this->individualUpdateChecks;
        $defaultBStats = $this->bStats;
        $defaultNetwork = $this->network;
        $defaultHttp = $this->httpServer;
        $defaultHttpClient = $this->httpClient;
        $defaultMySql = $this->mysqlSettings;

        ExceptionHandler::require(function (array $defaultUpdateChecks, array $defaultBStats, array $defaultNetwork, array $defaultHttp, array $defaultHttpClient, array $defaultMySql): void {
            $this->load();

            Utils::fillMissingKeys($this->individualUpdateChecks, $defaultUpdateChecks);
            Utils::fillMissingKeys($this->bStats, $defaultBStats);
            Utils::fillMissingKeys($this->network, $defaultNetwork);
            Utils::fillMissingKeys($this->httpServer, $defaultHttp);
            Utils::fillMissingKeys($this->httpClient, $defaultHttpClient);
            Utils::fillMissingKeys($this->mysqlSettings, $defaultMySql);

            if (!in_array(strtolower($this->provider), ["mysql", "json"])) {
                $this->provider = "json";
            }

            $this->save();
        }, "Failed to load main config", fn() => PocketCloud::getInstance()->shutdown(), $defaultUpdateChecks, $defaultBStats, $defaultNetwork, $defaultHttp, $defaultHttpClient, $defaultMySql);
    }

    public function getGeneratedKey(): string {
        return $this->generatedKey;
    }

    public function setCloudName(string $cloudName): void {
        $this->cloudName = $cloudName;
    }

    public function getCloudName(): string {
        return $this->cloudName;
    }

    public function setMemoryLimit(int $memoryLimit): MainConfig {
        $this->memoryLimit = $memoryLimit;
        return $this;
    }

    public function getMemoryLimit(): int {
        return $this->memoryLimit;
    }

    public function setLanguage(string $language): MainConfig {
        $this->language = $language;
        return $this;
    }

    public function getLanguage(): string {
        return $this->language;
    }

    public function setProvider(string $provider): MainConfig {
        $this->provider = $provider;
        return $this;
    }

    public function getProvider(): string {
        return $this->provider;
    }

    public function canCheckForUpdates(IUpdateChecker|string $type): bool {
        $type = $type instanceof IUpdateChecker ? $type->id() : $type;
        if (!isset($this->individualUpdateChecks[$type])) return false;
        return $this->individualUpdateChecks[$type]["check"] ?? false;
    }

    public function canUpdate(IUpdateChecker|string $type): bool {
        $type = $type instanceof IUpdateChecker ? $type->id() : $type;
        if (!isset($this->individualUpdateChecks[$type])) return false;
        return $this->individualUpdateChecks[$type]["update"] ?? false;
    }

    public function setStartUpDelay(bool $startUpDelay): MainConfig {
        $this->startUpDelay = $startUpDelay;
        return $this;
    }

    public function isStartUpDelay(): bool {
        return $this->startUpDelay;
    }

    public function setWriteTimingsOnShutdown(bool $writeTimingsOnShutdown): MainConfig {
        $this->writeTimingsOnShutdown = $writeTimingsOnShutdown;
        return $this;
    }

    public function isWriteTimingsOnShutdown(): bool {
        return $this->writeTimingsOnShutdown;
    }

    public function setBStatsEnabled(bool $enabled): MainConfig {
        $this->bStats["enabled"] = $enabled;
        return $this;
    }

    public function setBStatsLogFailedRequests(bool $value): MainConfig {
        $this->bStats["log_failed_requests"] = $value;
        return $this;
    }

    public function setBStatsLogSentData(bool $value): MainConfig {
        $this->bStats["log_sent_data"] = $value;
        return $this;
    }

    public function setBStatsLogResponseStatusText(bool $value): MainConfig {
        $this->bStats["log_response_status_text"] = $value;
        return $this;
    }

    public function setBStats(array $bStats): MainConfig {
        Utils::validateArraySignature($bStats, [
            "enabled" => "bool",
            "log_failed_requests" => "bool",
            "log_sent_data" => "bool",
            "log_response_status_text" => "bool"
        ]);

        $this->bStats = $bStats;
        return $this;
    }

    public function isBStatsEnabled(): bool {
        return $this->bStats["enabled"];
    }

    public function isBStatsLogFailedRequests(): bool {
        return $this->bStats["log_failed_requests"];
    }

    public function isBStatsLogSentData(): bool {
        return $this->bStats["log_sent_data"];
    }

    public function isBStatsLogResponseStatusText(): bool {
        return $this->bStats["log_response_status_text"];
    }

    public function getBStats(): array {
        return $this->bStats;
    }

    public function setNetworkAddress(string $address): MainConfig {
        $this->network["address"] = $address;
        return $this;
    }

    public function setNetworkPort(int $port): MainConfig {
        if ($port < 1 || $port > 65535) throw new InvalidArgumentException("Invalid network port");
        $this->network["port"] = $port;
        return $this;
    }

    public function setNetworkEncryption(bool $enabled): MainConfig {
        $this->network["encryption"] = $enabled;
        return $this;
    }

    public function setNetworkPacketSizeLimit(int $bytes): MainConfig {
        if ($bytes < 1024) throw new InvalidArgumentException("Invalid network packet size, must be greater or equals to 1024 bytes");
        $this->network["packet_size_limit"] = $bytes;
        return $this;
    }

    public function setNetwork(array $network): MainConfig {
        Utils::validateArraySignature($network, [
            "address" => "string",
            "port" => "int",
            "encryption" => "bool",
            "packet_size_limit" => "int"
        ]);

        $this->network = $network;
        return $this;
    }

    public function getNetworkAddress(): string {
        return $this->network["address"];
    }

    public function getNetworkPort(): int {
        return $this->network["port"];
    }

    public function isNetworkEncryptionEnabled(): bool {
        return $this->network["encryption"];
    }

    public function getNetworkPacketSizeLimit(): int {
        return $this->network["packet_size_limit"];
    }

    public function getNetwork(): array {
        return $this->network;
    }

    public function setHttpServerEnabled(bool $enabled): MainConfig {
        $this->httpServer["enabled"] = $enabled;
        return $this;
    }

    public function setHttpServerAddress(string $address): MainConfig {
        $this->httpServer["address"] = $address;
        return $this;
    }

    public function setHttpServerPort(int $port): MainConfig {
        if ($port < 1 || $port > 65535) throw new InvalidArgumentException("Invalid HTTP port");
        $this->httpServer["port"] = $port;
        return $this;
    }

    public function setHttpServerAuthKey(string $key): MainConfig {
        if ($key === "") throw new InvalidArgumentException("Auth key cannot be empty");
        $this->httpServer["auth-key"] = $key;
        return $this;
    }

    public function setHttpServerOnlyLocal(bool $value): MainConfig {
        $this->httpServer["only-local"] = $value;
        return $this;
    }

    public function setHttpRateLimitEnabled(bool $enabled): MainConfig {
        $this->httpServer["rate-limit"]["enabled"] = $enabled;
        return $this;
    }

    public function setHttpRateLimitTimeout(int $seconds): MainConfig {
        if ($seconds < 1) throw new InvalidArgumentException("Timeout must be >= 1");
        $this->httpServer["rate-limit"]["timeout_in_seconds"] = $seconds;
        return $this;
    }

    public function setHttpRateLimitMaxRequests(int $max): MainConfig {
        if ($max < 1) throw new InvalidArgumentException("Max requests must be >= 1");
        $this->httpServer["rate-limit"]["max_requests"] = $max;
        return $this;
    }

    public function setHttpRateLimitTimeFrame(int $seconds): MainConfig {
        if ($seconds < 1) throw new InvalidArgumentException("Time frame must be >= 1");
        $this->httpServer["rate-limit"]["time_frame_in_seconds"] = $seconds;
        return $this;
    }

    public function setHttpResponseCachingEnabled(bool $enabled): MainConfig {
        $this->httpServer["response-caching"]["enabled"] = $enabled;
        return $this;
    }

    public function setHttpResponseCachingTime(int $seconds): MainConfig {
        if ($seconds < 1) throw new InvalidArgumentException("Caching time must be >= 1");
        $this->httpServer["response-caching"]["caching_time_in_seconds"] = $seconds;
        return $this;
    }

    public function setHttpServer(array $httpServer): MainConfig {
        Utils::validateArraySignature($httpServer, [
            "enabled" => "bool",
            "address" => "string",
            "port" => "int",
            "auth-key" => "string",
            "only-local" => "bool",
            "rate-limit" => [
                "enabled" => "bool",
                "timeout_in_seconds" => "int",
                "max_requests" => "int",
                "time_frame_in_seconds" => "int"
            ],
            "response-caching" => [
                "enabled" => "bool",
                "caching_time_in_seconds" => "int"
            ]
        ]);

        $this->httpServer = $httpServer;
        return $this;
    }

    public function isHttpServerEnabled(): bool {
        return $this->httpServer["enabled"];
    }

    public function getHttpServerAddress(): string {
        return $this->httpServer["address"];
    }

    public function getHttpServerPort(): int {
        return $this->httpServer["port"];
    }

    public function getHttpServerAuthKey(): string {
        return $this->httpServer["auth-key"];
    }

    public function isHttpServerOnlyLocal(): bool {
        return $this->httpServer["only-local"];
    }

    public function getHttpRateLimitConfiguration(): array {
        return $this->httpServer["rate-limit"];
    }

    public function isHttpRateLimitEnabled(): bool {
        return $this->httpServer["rate-limit"]["enabled"];
    }

    public function getHttpRateLimitTimeout(): int {
        return $this->httpServer["rate-limit"]["timeout_in_seconds"];
    }

    public function getHttpRateLimitMaxRequests(): int {
        return $this->httpServer["rate-limit"]["max_requests"];
    }

    public function getHttpRateLimitTimeFrame(): int {
        return $this->httpServer["rate-limit"]["time_frame_in_seconds"];
    }

    public function getHttpResponseCachingConfiguration(): array {
        return $this->httpServer["response-caching"];
    }

    public function isHttpResponseCachingEnabled(): bool {
        return $this->httpServer["response-caching"]["enabled"];
    }

    public function getHttpResponseCachingTime(): int {
        return $this->httpServer["response-caching"]["caching_time_in_seconds"];
    }

    public function getHttpServer(): array {
        return $this->httpServer;
    }

    public function setHttpClientThreadCount(int $threadCount): MainConfig {
        if ($threadCount < 0) $threadCount = 0;
        $this->httpClient["thread-count"] = $threadCount;
        return $this;
    }

    public function setHttpClient(array $httpClient): MainConfig {
        Utils::validateArraySignature($httpClient, [
            "thread-count" => "int"
        ]);

        $this->httpClient = $httpClient;
        return $this;
    }

    public function getHttpClientThreadCount(): int {
        return $this->httpClient["thread-count"];
    }

    public function getHttpClient(): array {
        return $this->httpClient;
    }

    public function setMysqlAddress(string $address): MainConfig {
        $this->mysqlSettings["address"] = $address;
        return $this;
    }

    public function setMysqlPort(int $port): MainConfig {
        if ($port < 1 || $port > 65535) throw new InvalidArgumentException("Invalid MySQL port");
        $this->mysqlSettings["port"] = $port;
        return $this;
    }

    public function setMysqlUser(string $user): MainConfig {
        if ($user === "") throw new InvalidArgumentException("MySQL user cannot be empty");
        $this->mysqlSettings["user"] = $user;
        return $this;
    }

    public function setMysqlPassword(string $password): MainConfig {
        $this->mysqlSettings["password"] = $password;
        return $this;
    }

    public function setMysqlDatabase(string $database): MainConfig {
        if ($database === "") throw new InvalidArgumentException("Database cannot be empty");
        $this->mysqlSettings["database"] = $database;
        return $this;
    }

    public function setMysqlSettings(array $mysqlSettings): MainConfig {
        Utils::validateArraySignature($mysqlSettings, [
            "address" => "string",
            "port" => "int",
            "user" => "string",
            "password" => "string",
            "database" => "string"
        ]);

        $this->mysqlSettings = $mysqlSettings;
        return $this;
    }

    public function getMysqlAddress(): string {
        return $this->mysqlSettings["address"];
    }

    public function getMysqlPort(): int {
        return $this->mysqlSettings["port"];
    }

    public function getMysqlUser(): string {
        return $this->mysqlSettings["user"];
    }

    public function getMysqlPassword(): string {
        return $this->mysqlSettings["password"];
    }

    public function getMysqlDatabase(): string {
        return $this->mysqlSettings["database"];
    }

    public function getMysqlSettings(): array {
        return $this->mysqlSettings;
    }
}