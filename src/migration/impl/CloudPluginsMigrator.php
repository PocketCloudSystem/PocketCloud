<?php

namespace pocketcloud\cloud\migration\impl;

use pocketcloud\cloud\migration\IMigrator;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;
use const pocketcloud\BACKUPS_PATH;
use const pocketcloud\PLUGINS_PATH;
use const pocketcloud\STORAGE_PATH;

final class CloudPluginsMigrator implements IMigrator {

    private ?string $currentBackupId = null;
    private ?string $currentBackupPath = null;

    public function requiresMigration(): bool {
        return is_dir(PathUtils::join(STORAGE_PATH, "plugins", "cloud"));
    }

    public function backup(): bool {
        $this->currentBackupId = "backup-" . date("Y-m-d_H-i-s-v") . "-" . $this->id() . "-" . uniqid();
        $this->currentBackupPath = PathUtils::join(BACKUPS_PATH, $this->currentBackupId);
        if (!mkdir($this->currentBackupPath)) return false;
        return FileUtils::copyDirectory(PathUtils::join(STORAGE_PATH, "plugins", "cloud"), $this->currentBackupPath);
    }

    public function migrate(): bool {
        $successful = false;
        if (is_dir($cloudPluginsPath = PathUtils::join(STORAGE_PATH, "plugins", "cloud"))) {
            $successful = FileUtils::copyDirectory($cloudPluginsPath, PLUGINS_PATH);
            FileUtils::removeDirectory($cloudPluginsPath);
        }

        return $successful;
    }

    public function rollback(): bool {
        FileUtils::createDir(PathUtils::join(STORAGE_PATH, "plugins", "cloud"));
        return FileUtils::copyDirectory($this->currentBackupPath, PathUtils::join(STORAGE_PATH, "plugins", "cloud"));
    }

    public function id(): string {
        return "migrate-v3-cloud-plugins";
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