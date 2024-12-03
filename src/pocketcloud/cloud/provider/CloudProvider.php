<?php

namespace pocketcloud\cloud\provider;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\promise\Promise;

abstract class CloudProvider {

    private static ?self $current = null;

    abstract public function addTemplate(Template $template): void;

    abstract public function removeTemplate(Template $template): void;

    abstract public function getTemplate(string $template): Promise;

    abstract public function checkTemplate(string $template): Promise;

    abstract public function getTemplates(): Promise;

    abstract public function setModuleState(string $module, bool $enabled): void;

    abstract public function getModuleState(string $module): Promise;

    abstract public function enablePlayerNotifications(string $player): void;

    abstract public function disablePlayerNotifications(string $player): void;

    abstract public function hasNotificationsEnabled(string $player): Promise;

    abstract public function addToWhitelist(string $player): void;

    abstract public function removeFromWhitelist(string $player): void;

    abstract public function isOnWhitelist(string $player): Promise;

    public static function select(): void {
        self::$current = match (MainConfig::getInstance()->getProvider()) {
            "mysql" => new CloudMySqlProvider(),
            default => new CloudJsonProvider()
        };
    }

    public static function current(): self {
        if (self::$current === null) self::select();
        return self::$current;
    }
}