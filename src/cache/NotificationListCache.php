<?php

namespace pocketcloud\cloud\cache;

use pocketcloud\cloud\network\packet\impl\NotificationListSyncPacket;

final class NotificationListCache {

    private static array $notificationList = [];

    /** @internal  */
    public static function sync(array $notificationList): void {
        foreach ($notificationList as $player) self::$notificationList[$player] = $player;
    }

    private static function syncOut(): void {
        NotificationListSyncPacket::create(self::getAll())->broadcastPacket();
    }

    public static function add(string $player): void {
        if (self::is($player)) return;
        self::$notificationList[$player] = $player;
        self::syncOut();
    }

    public static function remove(string $player): void {
        if (!self::is($player)) return;
        unset(self::$notificationList[$player]);
        self::syncOut();
    }

    public static function is(string $player): bool {
        return isset(self::$notificationList[$player]);
    }

    public static function getAll(): array {
        return array_values(self::$notificationList);
    }
}