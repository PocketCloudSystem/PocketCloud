<?php

namespace pocketcloud\cloud\cache\impl;

use pocketcloud\cloud\cache\Cache;
use pocketcloud\cloud\network\packet\impl\MaintenanceListSyncPacket;

final class MaintenanceListCache implements Cache {

    private static array $maintenanceList = [];

    /** @internal  */
    public static function sync(array $maintenanceList): void {
        foreach ($maintenanceList as $player) self::$maintenanceList[$player] = $player;
    }

    /** @internal  */
    public static function syncOut(): void {
        MaintenanceListSyncPacket::create(self::getAll())->broadcastPacket();
    }

    public static function add(string $player): void {
        if (self::is($player)) return;
        self::$maintenanceList[$player] = $player;
        self::syncOut();
    }

    public static function remove(string $player): void {
        if (!self::is($player)) return;
        unset(self::$maintenanceList[$player]);
        self::syncOut();
    }

    public static function is(string $player): bool {
        return isset(self::$maintenanceList[$player]);
    }

    public static function getAll(): array {
        return array_values(self::$maintenanceList);
    }
}