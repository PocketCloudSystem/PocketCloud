<?php

namespace pocketcloud\cloud\config\impl;

use configlib\Configuration;
use InvalidArgumentException;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\network\packet\data\NotificationType;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\util\trait\SingletonTrait;
use pocketcloud\cloud\util\Utils;
use r3pt1s\discord\webhook\Webhook;
use const pocketcloud\STORAGE_PATH;

final class LogSettingsConfig extends Configuration {
    use SingletonTrait;

    /** @ignored */
    public const string CATEGORY_CONNECTION = "connection_lifecycle";
    /** @ignored */
    public const string CATEGORY_FAILED_JOINS = "failed_joins";
    /** @ignored */
    public const string CATEGORY_KICKS = "kicks";
    /** @ignored */
    public const string CATEGORY_SERVER_SWITCHED = "server_switched";

    /** @ignored */
    private ?Webhook $webhook = null;

    /**
     * @comment Whether you want to see debug logs or not
     */
    private bool $debugMode = false;

    private array $discordWebhook = [
        "enabled" => false,
        "webhook-url" => "your-discord-webhook-url",
        "notifications" => [
            "crashed_servers" => true,
            "timed_out_servers" => true,
            "failed_server_starts" => true,
            "failed_player_joins" => true,
            "player_kicks" => true
        ]
    ];

    /**
     * @comment Here you can decide whether you want to see / be notified when players join, leave, get kicked, cannot join, switch servers, ...
     * @comment connection_lifecycle => regular player join/leave messages
     * @comment failed_joins => fires when a player gets kicked during his login sequence (e.g. not whitelisted, through a plugin, incompatible version, ...)
     * @comment kicks => regular kick via ingame or cloud messages
     * @comment server_switched => regular player server switching messages
     * @comment -----------------------
     * @comment console: true -> means the logs will be displayed inside the console
     * @comment in_game: true -> means players who have their cloud notifications on will receive the message / notification
     */
    private array $playerLogs = [
        "connection_lifecycle" => [
            "console" => true,
            "in_game" => true
        ],
        "failed_joins" => [
            "console" => true,
            "in_game" => true
        ],
        "kicks" => [
            "console" => true,
            "in_game" => true
        ],
        "server_switched" => [
            "console" => true,
            "in_game" => true
        ]
    ];

    public function __construct() {
        parent::__construct(STORAGE_PATH . "log_settings.yml", self::TYPE_YAML);
        self::setInstance($this);

        $defaultPlayerLogs = $this->playerLogs;
        $defaultDiscordWebhookSettings = $this->discordWebhook;
        ExceptionHandler::require(function (array $defaultPlayerLogs, array $defaultDiscordWebhookSettings): void {
            $this->load();

            Utils::fillMissingKeys($this->playerLogs, $defaultPlayerLogs);
            Utils::fillMissingKeys($this->discordWebhook, $defaultDiscordWebhookSettings);

            if (!filter_var($this->discordWebhook["webhook-url"], FILTER_VALIDATE_URL) && $this->discordWebhook["enabled"]) {
                Server::getInstance()->addStartNotification("Invalid webhook url inside §b{}§r. Resetting to default value...", CloudLogLevel::WARN(), $this->discordWebhook["webhook-url"]);
                $this->discordWebhook["webhook-url"] = $defaultDiscordWebhookSettings["webhook-url"];
            }

            CloudLogger::get()->setDebugMode($this->debugMode);

            $this->save();
        }, "Failed to load log settings config", fn() => Server::getInstance()->shutdown(), $defaultPlayerLogs, $defaultDiscordWebhookSettings);
    }

    private function assertCategory(string $category): void {
        if (!isset($this->playerLogs[$category])) throw new InvalidArgumentException("Unknown player log category: $category");
    }

    public function getWebhook(): ?Webhook {
        if ($this->discordWebhook["enabled"] && filter_var($this->discordWebhook["webhook-url"], FILTER_VALIDATE_URL)) {
            $this->webhook = new Webhook($this->discordWebhook["webhook-url"]);
        }

        return $this->webhook;
    }

    public function setDebugMode(bool $debugMode): void {
        $this->debugMode = $debugMode;
        CloudLogger::get()->setDebugMode($debugMode);
    }

    public function isDebugMode(): bool {
        return $this->debugMode;
    }

    public function setDiscordWebhookEnabled(bool $enabled): self {
        $this->discordWebhook["enabled"] = $enabled;
        return $this;
    }

    public function setDiscordWebhookUrl(?string $url): self {
        if ($url === null) {
            $this->discordWebhook["webhook-url"] = "your-discord-webhook-url";
        } else {
            if (filter_var($url, FILTER_VALIDATE_URL)) {
                $this->discordWebhook["webhook-url"] = $url;
            } else throw new InvalidArgumentException("Invalid webhook url: $url");
        }

        return $this;
    }

    public function setDiscordWebhookNotification(string $type, bool $enabled): self {
        $this->discordWebhook["notifications"][$type] = $enabled;
        return $this;
    }

    public function setDiscordWebhookNotifications(array $notifications): self {
        Utils::validateArraySignature($notifications, [
            "enabled" => "bool",
            "webhook-url" => "string",
            "notifications" => [
                "crashed_servers" => "bool",
                "timed_out_servers" => "bool",
                "failed_server_starts" => "bool",
                "failed_player_joins" => "bool",
                "player_kicks" => "bool"
            ]
        ], true);

        $this->discordWebhook["notifications"] = $notifications;
        return $this;
    }

    public function canSendWebhook(NotificationType $type): bool {
        $crashedServers = $this->discordWebhook["notifications"]["crashed_servers"];
        $timedOutServers = $this->discordWebhook["notifications"]["timed_out_servers"];
        $failedServerStarts = $this->discordWebhook["notifications"]["failed_server_starts"];
        $failedPlayerJoins = $this->discordWebhook["notifications"]["failed_player_joins"];
        $playerKicks = $this->discordWebhook["notifications"]["player_kicks"];

        return match ($type) {
            NotificationType::SERVER_CRASHED => $crashedServers,
            NotificationType::SERVER_TIMED_OUT, NotificationType::SERVER_STOP_TIMED_OUT => $timedOutServers,
            NotificationType::SERVER_START_FAILED => $failedServerStarts,
            NotificationType::PLAYER_JOIN_FAILED => $failedPlayerJoins,
            NotificationType::PLAYER_KICKED => $playerKicks,
            default => false
        };
    }

    public function getDiscordWebhook(): array {
        return $this->discordWebhook;
    }

    public function setPlayerLogCategory(string $category, bool $console, bool $inGame): self {
        $this->assertCategory($category);

        $this->playerLogs[$category] = [
            "console" => $console,
            "in_game" => $inGame
        ];

        return $this;
    }

    public function setPlayerLogs(array $playerLogs): void {
        Utils::validateArraySignature($playerLogs, [
            self::CATEGORY_CONNECTION => [
                "console" => "bool",
                "in_game" => "bool"
            ],
            self::CATEGORY_FAILED_JOINS => [
                "console" => "bool",
                "in_game" => "bool"
            ],
            self::CATEGORY_KICKS => [
                "console" => "bool",
                "in_game" => "bool"
            ],
            self::CATEGORY_SERVER_SWITCHED => [
                "console" => "bool",
                "in_game" => "bool"
            ]
        ], true);

        $this->playerLogs = $playerLogs;
    }

    public function canNotify(NotificationType $type): bool {
        $connectionLifecycle = $this->playerLogs[self::CATEGORY_CONNECTION]["in_game"];
        $failedJoins = $this->playerLogs[self::CATEGORY_FAILED_JOINS]["in_game"];
        $kicks = $this->playerLogs[self::CATEGORY_KICKS]["in_game"];
        $serverSwitched = $this->playerLogs[self::CATEGORY_SERVER_SWITCHED]["in_game"];

        return match ($type) {
            NotificationType::PLAYER_JOINED, NotificationType::PLAYER_LEFT => $connectionLifecycle,
            NotificationType::PLAYER_JOIN_FAILED => $failedJoins,
            NotificationType::PLAYER_KICKED => $kicks,
            NotificationType::PLAYER_SWITCHED_SERVER => $serverSwitched,
            default => true
        };
    }

    public function canLog(NotificationType $type): bool {
        $connectionLifecycle = $this->playerLogs[self::CATEGORY_CONNECTION]["console"];
        $failedJoins = $this->playerLogs[self::CATEGORY_FAILED_JOINS]["console"];
        $kicks = $this->playerLogs[self::CATEGORY_KICKS]["console"];
        $serverSwitched = $this->playerLogs[self::CATEGORY_SERVER_SWITCHED]["console"];

        return match ($type) {
            NotificationType::PLAYER_JOINED, NotificationType::PLAYER_LEFT => $connectionLifecycle,
            NotificationType::PLAYER_JOIN_FAILED => $failedJoins,
            NotificationType::PLAYER_KICKED => $kicks,
            NotificationType::PLAYER_SWITCHED_SERVER => $serverSwitched,
            default => true
        };
    }

    public function getPlayerLogs(): array {
        return $this->playerLogs;
    }
}