<?php

namespace pocketcloud\cloud\migration\impl;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\migration\IMigrator;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;
use r3pt1s\mysql\ConnectionPool;
use const pocketcloud\BACKUPS_PATH;
use const pocketcloud\SERVER_GROUPS_PATH;

final class JsonGroupsToMySqlMigrator implements IMigrator {

    private ?string $currentBackupId = null;
    private ?string $currentBackupPath = null;

    public function requiresMigration(): bool {
        return file_exists(PathUtils::join(SERVER_GROUPS_PATH, "groups.json"));
    }

    public function backup(): bool {
        $this->currentBackupId = "backup-" . date("Y-m-d_H-i-s-v") . "-" . $this->id() . "-" . uniqid();
        $this->currentBackupPath = PathUtils::join(BACKUPS_PATH, $this->currentBackupId);
        if (!mkdir($this->currentBackupPath)) return false;
        return FileUtils::copyFile(PathUtils::join(SERVER_GROUPS_PATH, "groups.json"), PathUtils::join($this->currentBackupPath, "groups.json"));
    }

    public function migrate(): bool {
        $jsonContent = FileUtils::decodeJsonFile(PathUtils::join(SERVER_GROUPS_PATH, "groups.json"));
        if ($jsonContent === false) return false;
        FileUtils::unlinkFile(PathUtils::join(SERVER_GROUPS_PATH, "groups.json"));
        ConnectionPool::getInstance()->enableSyncQueries();
        $available = 0;
        $successful = 0;
        foreach ($jsonContent as $i => $item) {
            if (($group = ServerGroup::read($item)) !== null) {
                $available++;
                CloudProvider::current()->addServerGroup($group)
                    ->then(function () use(&$successful): void {
                        $successful++;
                    });
            } else CloudLogger::get()->warn("Skipped server group §b{} §ron migration", $i);
        }

        ConnectionPool::getInstance()->enableSyncQueries(false);
        return $successful == $available;
    }

    public function rollback(): bool {
        return FileUtils::copyFile(PathUtils::join($this->currentBackupPath, "groups.json"), PathUtils::join(SERVER_GROUPS_PATH, "groups.json"));
    }

    public function id(): string {
        return "migrate-json-groups-to-mysql";
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