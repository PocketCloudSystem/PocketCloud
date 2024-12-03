<?php

namespace pocketcloud\cloud\provider;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\promise\Promise;

abstract class CloudProvider {

    private static ?self $current = null;

    abstract public function createTemplate(Template $template): void;

    abstract public function removeTemplate(Template $template): void;

    abstract public function getTemplate(string $template): Promise;

    abstract public function checkTemplate(string $template): Promise;

    abstract public function getTemplates(): Promise;

    protected static function select(): void {
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