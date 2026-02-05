<?php

namespace pocketcloud\cloud\migration\impl;

use pocketcloud\cloud\migration\IMigrator;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;
use r3pt1s\mysql\ConnectionPool;
use const pocketcloud\BACKUPS_PATH;
use const pocketcloud\IN_GAME_PATH;

final class JsonNotificationsToMySqlMigrator implements IMigrator {

    private ?string $currentBackupId = null;
    private ?string $currentBackupPath = null;

    public function requiresMigration(): bool {
        return file_exists(PathUtils::join(IN_GAME_PATH, "notifications.json"));
    }

    public function backup(): bool {
        $this->currentBackupId = "backup-" . date("Y-m-d_H-i-s-v") . "-" . $this->id() . "-" . uniqid();
        $this->currentBackupPath = PathUtils::join(BACKUPS_PATH, $this->currentBackupId);
        if (!mkdir($this->currentBackupPath)) return false;
        return FileUtils::copyFile(PathUtils::join(IN_GAME_PATH, "notifications.json"), PathUtils::join($this->currentBackupPath, "notifications.json"));
    }

    public function migrate(): bool {
        $jsonContent = FileUtils::decodeJsonFile(PathUtils::join(IN_GAME_PATH, "notifications.json"));
        if ($jsonContent === false) return false;
        FileUtils::unlinkFile(PathUtils::join(IN_GAME_PATH, "notifications.json"));
        ConnectionPool::getInstance()->enableSyncQueries();
        foreach ($jsonContent as $key => $enabled) {
            CloudProvider::current()->{($enabled ? "enablePlayerNotifications" : "disablePlayerNotifications")}($key);
        }

        ConnectionPool::getInstance()->enableSyncQueries(false);
        return true;
    }

    public function rollback(): bool {
        return FileUtils::copyFile(PathUtils::join($this->currentBackupPath, "notifications.json"), PathUtils::join(IN_GAME_PATH, "notifications.json"));
    }

    public function id(): string {
        return "migrate-json-notifications-to-mysql";
    }

    public function currentBackupId(): string {
        return $this->currentBackupId;
    }

    public function currentBackupPath(): string {
        return $this->currentBackupPath;
    }

    public function runOnStartup(): bool {
        return false;
    }
}