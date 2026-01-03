<?php

namespace pocketcloud\cloud\cache;

use pocketcloud\cloud\network\packet\impl\MaintenanceListSyncPacket;

final class MaintenanceListCache {

    private static array $maintenanceList = [];

    /** @internal  */
    public static function sync(array $maintenanceList): void {
        foreach ($maintenanceList as $player) self::$maintenanceList[$player] = true;
    }

    private static function syncOut(): void {
        MaintenanceListSyncPacket::create(self::getAll())->broadcastPacket();
    }

    public static function add(string $player): void {
        self::$maintenanceList[$player] = true;
        self::syncOut();
    }

    public static function remove(string $player): void {
        if (self::is($player)) {
            unset(self::$maintenanceList[$player]);
            self::syncOut();
        }
    }

    public static function is(string $player): bool {
        return self::$maintenanceList[$player] ?? false;
    }

    public static function getAll(): array {
        return array_keys(self::$maintenanceList);
    }
}