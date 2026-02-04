<?php

namespace pocketcloud\cloud\migration\impl;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\migration\IMigrator;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;
use const pocketcloud\BACKUPS_PATH;
use const pocketcloud\TEMPLATES_PATH;

final class JsonTemplatesToMySqlMigrator implements IMigrator {

    private ?string $currentBackupId = null;
    private ?string $currentBackupPath = null;

    public function requiresMigration(): bool {
        return file_exists(PathUtils::join(TEMPLATES_PATH, "templates.json"));
    }

    public function backup(): bool {
        $this->currentBackupId = "backup-" . date("Y-m-d_H-i-s-v") . "-" . $this->id() . "-" . uniqid();
        $this->currentBackupPath = PathUtils::join(BACKUPS_PATH, $this->currentBackupId);
        if (!mkdir($this->currentBackupPath)) return false;
        return FileUtils::copyFile(PathUtils::join(TEMPLATES_PATH, "templates.json"), PathUtils::join($this->currentBackupPath, "templates.json"));
    }

    public function migrate(): bool {
        $jsonContent = FileUtils::decodeJsonFile(PathUtils::join(TEMPLATES_PATH, "templates.json"));
        if ($jsonContent === false) return false;
        FileUtils::unlinkFile(PathUtils::join(TEMPLATES_PATH, "templates.json"));
        $successful = 0;
        foreach ($jsonContent as $i => $item) {
            if (($template = Template::read($item)) !== null) {
                CloudProvider::current()->addTemplate($template);
            } else CloudLogger::get()->warn("Skipped template §b{} §ron migration", $i);
        }
    }

    public function rollback(): bool {}

    public function id(): string {
        return "migrate-json-templates-to-mysql";
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