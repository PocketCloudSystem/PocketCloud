<?php

namespace pocketcloud\cloud\migration\impl;

use pocketcloud\cloud\migration\IMigrator;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;
use const pocketcloud\BACKUPS_PATH;
use const pocketcloud\STORAGE_PATH;

final class ServerPluginsMigrator implements IMigrator {

    private ?string $currentBackupId = null;
    private ?string $currentBackupPath = null;

    public function requiresMigration(): bool {
        return is_dir(PathUtils::join(STORAGE_PATH, "plugins", "server"));
    }

    public function backup(): bool {
        $this->currentBackupId = "backup-" . date("Y-m-d_H-i-s-v") . "-" . $this->id() . "-" . uniqid();
        $this->currentBackupPath = PathUtils::join(BACKUPS_PATH, $this->currentBackupId);
        if (!mkdir($this->currentBackupPath)) return false;
        return FileUtils::copyDirectory(PathUtils::join(STORAGE_PATH, "plugins", "server"), $this->currentBackupPath);
    }

    public function migrate(): bool {
        $successful = false;
        if (is_dir($serverPluginsPath = PathUtils::join(STORAGE_PATH, "plugins", "server"))) {
            $successful = FileUtils::copyDirectory($serverPluginsPath, PathUtils::join(TemplateType::SERVER()->getGlobalTemplatePath(), "plugins"));
            FileUtils::removeDirectory($serverPluginsPath);
        }

        return $successful;
    }

    public function rollback(): bool {
        FileUtils::createDir(PathUtils::join(STORAGE_PATH, "plugins", "server"));
        return FileUtils::copyDirectory($this->currentBackupPath, PathUtils::join(STORAGE_PATH, "plugins", "server"));
    }

    public function id(): string {
        return "migrate-v3-server-plugins";
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