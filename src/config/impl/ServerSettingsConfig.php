<?php

namespace pocketcloud\cloud\config\impl;

use configlib\Configuration;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\server\util\ServerStartMethod;
use pocketcloud\cloud\util\trait\SingletonTrait;
use pocketcloud\cloud\util\Utils;
use Random\Randomizer;
use const pocketcloud\STORAGE_PATH;

final class ServerSettingsConfig extends Configuration {
    use SingletonTrait;

    /** @ignored */
    public const string TYPE_SERVER = "server";
    /** @ignored */
    public const string TYPE_PROXY = "proxy";

    /**
     * @comment Available start methods for servers are:
     * @comment tmux, screen, proc (process)
     */
    private string $startMethod = "tmux";

    /**
     * @comment By default, we are creating zero threads to save some resources.
     * @comment Recommended to use if you have a high amount of servers you want to start or large templates
     * @comment If set to 0, every template/ server group will be copied by the main-thread, increasing wait time
     * @comment (completely depends on your template size)
     */
    private int $serverPrepareThreads = 0;

    /**
     * @comment Custom binaries for template types, e.g. for servers (pmmp) you need the respective PHP Binary by pmmp
     */
    private array $binaries = [
        "server" => "https://github.com/pmmp/PHP-Binaries/releases/latest/download/PHP-{php_ver}-Linux-x86_64-PM5.tar.gz"
    ];

    /**
     * @comment These are the initial start commands for the servers (their template type)
     */
    private array $startCommands = [
        "server" => "%BINARY_PATH%bin/php7/bin/php %SOFTWARE_PATH%PocketMine-MP.phar --no-wizard",
        "proxy" => "java -jar %SOFTWARE_PATH%Waterdog.jar"
    ];

    /**
     * @comment Default for server is 15
     * @comment Default for proxy is 20
     */
    private array $serverTimeouts = [
        "server" => 15,
        "proxy" => 20
    ];

    /**
     * @comment Default port ranges for servers are 40000-65535, using random ports
     * @comment Default port ranges for proxies are 19132-20000, using fixed increments for ports (19132, 19133, ...)
     */
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

    public function __construct() {
        parent::__construct(STORAGE_PATH . "config.json", self::TYPE_YAML);
        self::setInstance($this);

        $defaultBinaries = $this->binaries;
        $defaultStartCommands = $this->startCommands;
        $defaultServerTimeouts = $this->serverTimeouts;
        $defaultServerPortRanges = $this->serverPortRanges;

        ExceptionHandler::tryCatch(function (array $defaultBinaries, array $defaultStartCommands, array $defaultServerTimeouts, array $defaultServerPortRanges): void {
            $this->load();
            foreach (array_keys($defaultBinaries) as $binary) {
                if (!isset($this->binaries[$binary])) $this->binaries[$binary] = str_replace(["{php_ver}"], [substr(PHP_VERSION, 0, 3)], $defaultBinaries[$binary]);
                else if ($this->binaries[$binary]) $this->binaries[$binary] = str_replace(["{php_ver}"], [substr(PHP_VERSION, 0, 3)], $this->binaries[$binary]);
            }

            Utils::fillMissingKeys($this->startCommands, $defaultStartCommands);
            Utils::fillMissingKeys($this->serverTimeouts, $defaultServerTimeouts);
            Utils::fillMissingKeys($this->serverPortRanges, $defaultServerPortRanges);

            if (!in_array(strtolower($this->startMethod), ["tmux", "screen", "proc"])) {
                $this->startMethod = "tmux";
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

                /**
                 * Re-setting this due to strict declarations
                 * @see ServerUtils
                 */
                $this->serverPortRanges[$key] = [
                    "start" => $start, "end" => $end, "random-ports" => $randomPorts
                ];
            }

            ServerStartMethod::set(ServerStartMethod::get($this->startMethod));

            $this->save();
        }, "Failed to load server settings config", fn() => PocketCloud::getInstance()->shutdown(), $defaultBinaries, $defaultStartCommands, $defaultServerTimeouts, $defaultServerPortRanges);
    }

    private function assertTemplateType(string $type): void {
        if (!in_array($type, [self::TYPE_SERVER, self::TYPE_PROXY], true)) throw new \InvalidArgumentException("Unknown template type: $type");
    }

    private function validatePortRange(int $start, int $end): void {
        if ($start <= 0 || $end <= 0) throw new \InvalidArgumentException("Ports must be > 0");
        if ($start > $end) throw new \InvalidArgumentException("Start port must be <= end port");
        if (($start + 50) > $end) throw new \InvalidArgumentException("Port range must contain at least 50 ports");
    }

    public function setStartMethod(string $startMethod): ServerSettingsConfig {
        $this->startMethod = $startMethod;
        return $this;
    }

    public function getStartMethod(): string {
        return $this->startMethod;
    }

    public function setServerPrepareThreads(int $serverPrepareThreads): ServerSettingsConfig {
        $this->serverPrepareThreads = $serverPrepareThreads;
        return $this;
    }

    public function getServerPrepareThreads(): int {
        return $this->serverPrepareThreads;
    }

    public function setBinary(string $type, string $url): self {
        $this->assertTemplateType($type);
        if ($url === "") throw new \InvalidArgumentException("Binary URL cannot be empty");
        $this->binaries[$type] = $url;
        return $this;
    }

    public function setBinaries(array $binaries): ServerSettingsConfig {
        Utils::validateArraySignature($binaries, ["server" => "string"], true);
        $this->binaries = $binaries;
        return $this;
    }

    public function getBinary(string $type): string {
        $this->assertTemplateType($type);
        return $this->binaries[$type];
    }

    public function getBinaries(): array {
        return $this->binaries;
    }

    public function setStartCommand(string $type, string $command): self {
        $this->assertTemplateType($type);
        if ($command === "") throw new \InvalidArgumentException("Start command cannot be empty");
        $this->startCommands[$type] = $command;
        return $this;
    }

    public function setStartCommands(array $startCommands): ServerSettingsConfig {
        Utils::validateArraySignature($startCommands, [
            "server" => "string",
            "proxy" => "string"
        ], true);

        $this->startCommands = $startCommands;
        return $this;
    }

    public function getStartCommand(string $type): string {
        $this->assertTemplateType($type);
        return $this->startCommands[$type];
    }

    public function getStartCommands(): array {
        return $this->startCommands;
    }

    public function setServerTimeout(string $type, int $seconds): self {
        $this->assertTemplateType($type);
        if ($seconds < 1) throw new \InvalidArgumentException("Timeout must be >= 1");
        $this->serverTimeouts[$type] = $seconds;
        return $this;
    }

    public function setServerTimeouts(array $serverTimeouts): ServerSettingsConfig {
        Utils::validateArraySignature($serverTimeouts, [
            "server" => "int",
            "proxy" => "int"
        ], true);

        $this->serverTimeouts = $serverTimeouts;
        return $this;
    }

    public function getServerTimeout(string $type): int {
        $this->assertTemplateType($type);
        return $this->serverTimeouts[$type];
    }

    public function getServerTimeouts(): array {
        return $this->serverTimeouts;
    }

    public function setPortRange(string $type, int $start, int $end, bool $random): self {
        $this->assertTemplateType($type);
        $this->validatePortRange($start, $end);

        $this->serverPortRanges[$type] = [
            "start" => $start,
            "end" => $end,
            "random-ports" => $random
        ];

        return $this;
    }

    public function setServerPortRanges(array $serverPortRanges): ServerSettingsConfig {
        Utils::validateArraySignature($serverPortRanges, [
            "server" => [
                "start" => "int",
                "end" => "int",
                "random-ports" => "bool"
            ],
            "proxy" => [
                "start" => "int",
                "end" => "int",
                "random-ports" => "bool"
            ]
        ], true);

        $this->serverPortRanges = $serverPortRanges;
        return $this;
    }

    public function getPortRangeStart(string $type): int {
        $this->assertTemplateType($type);
        return $this->serverPortRanges[$type]["start"];
    }

    public function getPortRangeEnd(string $type): int {
        $this->assertTemplateType($type);
        return $this->serverPortRanges[$type]["end"];
    }

    public function isPortRangeRandom(string $type): bool {
        $this->assertTemplateType($type);
        return $this->serverPortRanges[$type]["random-ports"];
    }

    public function getServerPortRanges(): array {
        return $this->serverPortRanges;
    }
}