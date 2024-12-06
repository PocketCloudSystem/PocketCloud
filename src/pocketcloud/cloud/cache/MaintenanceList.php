<?php

namespace pocketcloud\cloud\cache;

final class MaintenanceList {

    private static array $maintenanceList = [];

    /** @internal  */
    public static function sync(array $maintenanceList): void {
        foreach ($maintenanceList as $player) self::$maintenanceList[$player] = true;
    }

    public static function add(string $player): void {
        self::$maintenanceList[$player] = true;
    }

    public static function remove(string $player): void {
        if (self::is($player)) unset(self::$maintenanceList[$player]);
    }

    public static function is(string $player): bool {
        return self::$maintenanceList[$player] ?? false;
    }

    public static function getAll(): array {
        return array_keys(self::$maintenanceList);
    }
}