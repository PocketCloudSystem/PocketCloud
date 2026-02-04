<?php

namespace pocketcloud\cloud\migration\impl;

use pocketcloud\cloud\migration\IMigrator;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;
use const pocketcloud\BACKUPS_PATH;
use const pocketcloud\IN_GAME_PATH;

final class OldNotifyListToNotificationsMigrator implements IMigrator {

    private ?string $currentBackupId = null;
    private ?string $currentBackupPath = null;

    public function requiresMigration(): bool {
        return file_exists(PathUtils::join(IN_GAME_PATH, "notifyList.json"));
    }

    public function backup(): bool {
        $this->currentBackupId = "backup-" . date("Y-m-d_H-i-s-v") . "-" . $this->id() . "-" . uniqid();
        $this->currentBackupPath = PathUtils::join(BACKUPS_PATH, $this->currentBackupId);
        if (!mkdir($this->currentBackupPath)) return false;
        return FileUtils::copyFile(PathUtils::join(IN_GAME_PATH, "notifyList.json"), PathUtils::join($this->currentBackupPath, "notifyList.json"));
    }

    public function migrate(): bool {
        FileUtils::unlinkFile(PathUtils::join(IN_GAME_PATH, "notifyList.json"));
        return FileUtils::rename(PathUtils::join(IN_GAME_PATH, "notifyList.json"), PathUtils::join(IN_GAME_PATH, "notifications.json"));
    }

    public function rollback(): bool {
        return FileUtils::copyFile(PathUtils::join($this->currentBackupPath, "notifyList.json"), PathUtils::join(IN_GAME_PATH, "notifyList.json"));
    }

    public function id(): string {
        return "migrate-old-notifications-to-new-notifications";
    }

    public function currentBackupId(): string {
        return $this->currentBackupId;
    }

    public function currentBackupPath(): string {
        return $this->currentBackupPath;
    }

    public function runOnStartup(): bool {
        return true;
    }
}