<?php

namespace pocketcloud\cloud\provider;

use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\promise\Promise;

abstract class CloudProvider {

    private static ?self $current = null;

    abstract public function createTemplate(Template $template): Promise;

    abstract public function removeTemplate(Template $template): Promise;

    abstract public function getTemplate(string $template): Promise;

    abstract public function checkTemplate(string $template): Promise;

    abstract public function getTemplates(): Promise;

    protected static function select(): void {

    }

    public static function current(): self {
        if (self::$current === null) self::select();
        return self::$current;
    }
}