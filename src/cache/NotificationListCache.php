<?php

namespace pocketcloud\cloud\cache;

use pocketcloud\cloud\network\packet\impl\NotificationListSyncPacket;

final class NotificationListCache {

    private static array $notificationList = [];

    /** @internal  */
    public static function sync(array $notificationList): void {
        foreach ($notificationList as $player) self::$notificationList[$player] = true;
    }

    private static function syncOut(): void {
        NotificationListSyncPacket::create(self::getAll())->broadcastPacket();
    }

    public static function add(string $player): void {
        self::$notificationList[$player] = true;
        self::syncOut();
    }

    public static function remove(string $player): void {
        if (self::is($player)) {
            unset(self::$notificationList[$player]);
            self::syncOut();
        }
    }

    public static function is(string $player): bool {
        return self::$notificationList[$player] ?? false;
    }

    public static function getAll(): array {
        return array_keys(self::$notificationList);
    }
}