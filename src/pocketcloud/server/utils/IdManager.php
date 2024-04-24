<?php

namespace pocketcloud\server\utils;

use pocketcloud\template\Template;

class IdManager {

    private static array $ids = [];

    public static function addId(Template $template, int $id): void {
        if (isset(self::$ids[$template->getName()])) {
            if (!in_array($id, self::$ids[$template->getName()])) {
                self::$ids[$template->getName()][] = $id;
            }
        } else {
            self::$ids[$template->getName()] = [$id];
        }
    }

    public static function removeId(Template $template, int $id): void {
        if (isset(self::$ids[$template->getName()])) {
            if (in_array($id, self::$ids[$template->getName()])) {
                unset(self::$ids[$template->getName()][array_search($id, self::$ids[$template->getName()])]);
            }
        }
    }

    public static function getFreeId(Template $template): int {
        if (!isset(self::$ids[$template->getName()])) self::$ids[$template->getName()] = [];
        for ($i = 1; $i < ($template->getSettings()->getMaxServerCount() + 1); $i++) {
            if (!in_array($i, self::$ids[$template->getName()])) return $i;
        }
        return -1;
    }
}