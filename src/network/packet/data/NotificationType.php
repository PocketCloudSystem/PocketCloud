<?php

namespace pocketcloud\cloud\network\packet\data;

use pocketcloud\cloud\config\impl\LogSettingsConfig;
use pocketcloud\cloud\language\LanguageKey;
use pocketcloud\cloud\network\packet\impl\CloudNotificationPacket;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\misc\Writeable;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\trait\EnumHelperTrait;

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

    public function notify(array $args): Promise {
        if (!$this->canNotify()) return Promise::rejected("Disabled inside the config for " . $this->getName());
        // We want the notifications to be sent via the proxy

        //TODO: discord webhook

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