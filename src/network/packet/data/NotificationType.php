<?php

namespace pocketcloud\cloud\network\packet\data;

use pocketcloud\cloud\config\impl\LogSettingsConfig;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\language\LanguageKey;
use pocketcloud\cloud\network\packet\impl\CloudNotificationPacket;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\misc\Writeable;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\trait\EnumHelperTrait;
use r3pt1s\discord\webhook\message\embed\Embed;
use r3pt1s\discord\webhook\message\Message;
use r3pt1s\discord\webhook\Webhook;
use Throwable;

enum NotificationType implements Writeable {
    use EnumHelperTrait;

    case SERVER_STARTING;
    case SERVER_STOPPING;
    case SERVER_TIMED_OUT;
    case SERVER_STOP_TIMED_OUT;
    case SERVER_CRASHED;
    case SERVER_START_FAILED;
    case PLAYER_JOINED;
    case PLAYER_LEFT;
    case PLAYER_JOIN_FAILED;
    case PLAYER_KICKED;
    case PLAYER_SWITCHED_SERVER;

    public function notify(array $args, array $extraArgs = []): Promise {
        if (!$this->canNotify()) return Promise::rejected("Disabled inside the config for " . $this->getName());
        // We want the notifications to be sent via the proxy

        if ($this->canSendWebhook()) {
            $message = $this->craftDiscordMessage($args, $extraArgs);
            $webhook = LogSettingsConfig::getInstance()->getWebhook();
            if ($message instanceof Message && $webhook instanceof Webhook) {
                $message->sendWithDiffWebhook($webhook)
                    ->failure(function (array $res): void {
                        [$response, $code] = $res;
                        $response = ($response === false ? "An error occurred inside cURL" : ($response instanceof Throwable ? $response->getMessage() : $response));
                        CloudLogger::get()->error("§cFailed to spread notification to discord, responded with code §e{}§8: §e{}", $code, $response);
                    });
            }
        }

        return CloudNotificationPacket::create($this, $args)->broadcastPacket(...(count(CloudServerManager::getInstance()->getAll(...TemplateType::onlyProxy())) == 0 ? [] : TemplateType::onlyNonProxy()));
    }

    public function canSendWebhook(): bool {
        return LogSettingsConfig::getInstance()->canSendWebhook($this);
    }

    public function canNotify(): bool {
        return LogSettingsConfig::getInstance()->canNotify($this);
    }

    public function canLog(): bool {
        return LogSettingsConfig::getInstance()->canLog($this);
    }

    public function getName(): string {
        return $this->name;
    }

    public function craftDiscordMessage(array $args, array $extraArgs = []): ?Message {
        $message = new Message(false);
        $message->setUsername("PocketCloud Notifications | " . MainConfig::getInstance()->getCloudName());
        $message->setAvatarUrl("https://avatars.githubusercontent.com/u/97796660?s=400&u=a65bced92fb37ce5bafc5f1eff9e2845fe66a9cb&v=4");

        switch ($this) {
            case self::SERVER_CRASHED: {
                $crashData = $extraArgs["crashData"] ?? null;
                $message->addEmbed(Embed::create()
                    ->setTitle("Notification | Server Crash Report")
                    ->setDescription("`The cloud detected a crash on the following server:`")
                    ->setColor(0xFF0000)
                    ->addField("**Affected Server**", "> " . $args["server"])
                    ->setFooter("Notification Type: " . $this->name)
                );

                if ($crashData === null) {
                    $message->addEmbed(Embed::create()
                        ->setTitle("Crash Data")
                        ->setDescription("> No data available.")
                        ->setColor(0xFF0000)
                    );
                } else {
                    $crashTrace = isset($crashData["trace"]) ? implode("\n", $crashData["trace"]) : null;
                    $trace = substr($crashTrace ?? "No trace available", 0, 1000);
                    $errorData = $crashData["error"] ?? [];
                    $errorType = $errorData["type"] ?? "No error type found.";
                    $errorMessage = $errorData["message"] ?? "No error message found.";
                    $errorFile = $errorData["file"] ?? "No file found.";
                    $errorLine = $errorData["line"] ?? "No line found.";

                    $message->addEmbed(Embed::create()
                        ->setTitle("Crash Data")
                        ->addField("**Error Type**", "> " . $errorType, true)
                        ->addField("**File**", "> " . $errorFile . " (L: " . $errorLine . ")", true)
                        ->addField("**Message**", "> " . $errorMessage)
                        ->addField("**Trace**", "```php\n" . $trace . "\n```")
                        ->setColor(0xFF0000)
                    );
                }
                break;
            }
            case self::SERVER_START_FAILED: {
                $reason = $args["reason"] ?? null;

                if ($reason === null) {
                    $message->addEmbed(Embed::create()
                        ->setTitle("Notification | Server Start Failed")
                        ->setDescription("`The server exceeded the time to start, killed the created process. (Please take a look into this)`")
                        ->setColor(0xFF0000)
                        ->addField("**Affected Server**", "> " . $args["server"])
                        ->setFooter("Notification Type: " . $this->name)
                    );
                } else {
                    $message->addEmbed(Embed::create()
                        ->setTitle("Notification | Server Start Failed")
                        ->setDescription("`The server failed to start.`")
                        ->setColor(0xFF0000)
                        ->addField("**Affected Server**", "> " . $args["server"])
                        ->addField("**Reason**", "> " . $reason)
                        ->setFooter("Notification Type: " . $this->name)
                    );
                }

                break;
            }
            case self::SERVER_STOP_TIMED_OUT: {
                $message->addEmbed(Embed::create()
                    ->setTitle("Notification | Server Timeout")
                    ->setDescription("`The server exceeded the time to stop, killed the process.`")
                    ->setColor(0xFF0000)
                    ->addField("**Affected Server**", "> " . $args["server"])
                    ->setFooter("Notification Type: " . $this->name)
                );
                break;
            }
            case self::SERVER_TIMED_OUT: {
                $message->addEmbed(Embed::create()
                    ->setTitle("Notification | Server Timeout")
                    ->setDescription("`The server did not respond to the cloud ping, killed the process.`")
                    ->setColor(0xFF0000)
                    ->addField("**Affected Server**", "> " . $args["server"])
                    ->setFooter("Notification Type: " . $this->name)
                );
                break;
            }
            case self::PLAYER_JOIN_FAILED: {
                [$player, $server, $reason] = [$args["player"], $args["server"], $args["reason"]];
                $message->addEmbed(Embed::create()
                    ->setTitle("Notification | Player Join Failed")
                    ->setDescription("`The player tried to join but has been kicked during his login/ join.`")
                    ->setColor(0xFF0000)
                    ->addField("**Affected Player**", "> " . $player)
                    ->addField("**Initial Server**", "> " . $server)
                    ->addField("**Reason**", "> " . $reason)
                    ->setFooter("Notification Type: " . $this->name)
                );
                break;
            }
            case self::PLAYER_KICKED: {
                [$player, $server, $reason] = [$args["player"], $args["server"], $args["reason"]];
                $message->addEmbed(Embed::create()
                    ->setTitle("Notification | Player Kicked")
                    ->setDescription("`The player has been kicked.`")
                    ->setColor(0xFF0000)
                    ->addField("**Affected Player**", "> " . $player)
                    ->addField("**Server**", "> " . $server)
                    ->addField("**Reason**", "> " . $reason)
                    ->setFooter("Notification Type: " . $this->name)
                );
                break;
            }
            default: $message = null;
        }

        return $message;
    }

    public function getLangKey(): LanguageKey {
        return match ($this) {
            self::SERVER_STARTING => LanguageKey::INGAME_NOTIFY_MESSAGE_SERVER_STARTING(),
            self::SERVER_STOPPING => LanguageKey::INGAME_NOTIFY_MESSAGE_SERVER_STOPPING(),
            self::SERVER_TIMED_OUT => LanguageKey::INGAME_NOTIFY_MESSAGE_SERVER_TIMED_OUT(),
            self::SERVER_STOP_TIMED_OUT => LanguageKey::INGAME_NOTIFY_MESSAGE_SERVER_STOP_TIMED_OUT(),
            self::SERVER_CRASHED => LanguageKey::INGAME_NOTIFY_MESSAGE_SERVER_CRASHED(),
            self::SERVER_START_FAILED => LanguageKey::INGAME_NOTIFY_MESSAGE_SERVER_START_FAILED(),
            self::PLAYER_JOINED => LanguageKey::INGAME_NOTIFY_MESSAGE_PLAYER_JOINED(),
            self::PLAYER_LEFT => LanguageKey::INGAME_NOTIFY_MESSAGE_PLAYER_LEFT(),
            self::PLAYER_JOIN_FAILED => LanguageKey::INGAME_NOTIFY_MESSAGE_PLAYER_JOIN_FAILED(),
            self::PLAYER_KICKED => LanguageKey::INGAME_NOTIFY_MESSAGE_PLAYER_KICKED(),
            self::PLAYER_SWITCHED_SERVER => LanguageKey::INGAME_NOTIFY_MESSAGE_PLAYER_SWITCHED_SERVER()
        };
    }

    public function write(): string {
        return $this->name;
    }
}