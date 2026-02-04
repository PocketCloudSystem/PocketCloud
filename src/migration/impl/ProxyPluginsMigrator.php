<?php

namespace pocketcloud\cloud\migration\impl;

use pocketcloud\cloud\migration\IMigrator;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;
use const pocketcloud\BACKUPS_PATH;
use const pocketcloud\STORAGE_PATH;

final class ProxyPluginsMigrator implements IMigrator {

    private ?string $currentBackupId = null;
    private ?string $currentBackupPath = null;

    public function requiresMigration(): bool {
        return is_dir(PathUtils::join(STORAGE_PATH, "plugins", "proxy"));
    }

    public function backup(): bool {
        $this->currentBackupId = "backup-" . date("Y-m-d_H-i-s-v") . "-" . $this->id() . "-" . uniqid();
        $this->currentBackupPath = PathUtils::join(BACKUPS_PATH, $this->currentBackupId);
        if (!mkdir($this->currentBackupPath)) return false;
        return FileUtils::copyDirectory(PathUtils::join(STORAGE_PATH, "plugins", "proxy"), $this->currentBackupPath);
    }

    public function migrate(): bool {
        $successful = false;
        if (is_dir($proxyPluginsPath = PathUtils::join(STORAGE_PATH, "plugins", "proxy"))) {
            $successful = FileUtils::copyDirectory($proxyPluginsPath, PathUtils::join(TemplateType::PROXY()->getGlobalTemplatePath(), "plugins"));
            FileUtils::removeDirectory($proxyPluginsPath);
        }

        return $successful;
    }

    public function rollback(): bool {
        FileUtils::createDir(PathUtils::join(STORAGE_PATH, "plugins", "proxy"));
        return FileUtils::copyDirectory($this->currentBackupPath, PathUtils::join(STORAGE_PATH, "plugins", "proxy"));
    }

    public function id(): string {
        return "migrate-v3-proxy-plugins";
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