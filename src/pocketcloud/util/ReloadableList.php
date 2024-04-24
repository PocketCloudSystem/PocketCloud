<?php

namespace pocketcloud\util;

use pocketcloud\language\Language;
use ReflectionClass;
use Throwable;

class ReloadableList {

    /** @var array<Reloadable> */
    private static array $list = [];

    public static function add(Reloadable $reloadable): void {
        self::$list[spl_object_id($reloadable)] = $reloadable;
    }

    public static function remove(Reloadable $reloadable): void {
        if (isset(self::$list[spl_object_id($reloadable)])) unset(self::$list[spl_object_id($reloadable)]);
    }

    /** @internal */
    public static function reload(): void {
        foreach (self::$list as $reloadable) {
            try {
                if ($reloadable->reload()) {
                    CloudLogger::get()->debug("Reloaded " . (new ReflectionClass($reloadable))->getShortName());
                } else {
                    CloudLogger::get()->error(Language::current()->translate("reload.failed", (new ReflectionClass($reloadable))->getShortName()));
                }
            } catch (Throwable $exception) {
                CloudLogger::get()->error(Language::current()->translate("reload.failed", (new ReflectionClass($reloadable))->getShortName()));
                CloudLogger::get()->exception($exception);
            }
        }
    }

    public static function getList(): array {
        return self::$list;
    }
}