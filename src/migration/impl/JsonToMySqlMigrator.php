<?php

namespace pocketcloud\cloud\migration\impl;

use pocketcloud\cloud\cache\InGameModuleCache;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\migration\IMigrator;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\provider\database\DatabaseTables;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;
use r3pt1s\mysql\query\QueryBuilder;
use const pocketcloud\BACKUPS_PATH;
use const pocketcloud\IN_GAME_PATH;
use const pocketcloud\SERVER_GROUPS_PATH;
use const pocketcloud\TEMPLATES_PATH;

final class JsonToMySqlMigrator implements IMigrator {

    private ?string $currentBackupId = null;
    private ?string $currentBackupPath = null;

    public function requiresMigration(): bool {
        return file_exists(TEMPLATES_PATH . "templates.json") ||
            file_exists(SERVER_GROUPS_PATH . "groups.json") ||
            file_exists(IN_GAME_PATH . "maintenanceList.json") ||
            file_exists(IN_GAME_PATH . "modules.json") ||
            file_exists(IN_GAME_PATH . "notifications.json");
    }

    public function backup(): bool {
        $this->currentBackupId = "backup-" . date("Y-m-d_H-i-s-v") . "-" . $this->id() . "-" . uniqid();
        $this->currentBackupPath = PathUtils::join(BACKUPS_PATH, $this->currentBackupId);
        if (!mkdir($this->currentBackupPath)) return false;
        $available = [];
        $successful = 0;
        foreach ([TEMPLATES_PATH . "templates.json", SERVER_GROUPS_PATH . "groups.json", IN_GAME_PATH . "maintenanceList.json", IN_GAME_PATH . "modules.json", IN_GAME_PATH . "notifyList.json"] as $path) {
            if (file_exists($path)) $available[] = $path;
        }

        foreach ($available as $path) {
            $successful += (int) FileUtils::copyFile($path, PathUtils::join($this->currentBackupPath, basename($path)));
        }

        return $successful == count($available);
    }

    public function migrate(): bool {
        $successful = 0;
        $available = [];
        foreach ([TEMPLATES_PATH . "templates.json", SERVER_GROUPS_PATH . "groups.json", IN_GAME_PATH . "maintenanceList.json", IN_GAME_PATH . "modules.json", IN_GAME_PATH . "notifyList.json"] as $path) {
            if (file_exists($path)) $available[] = $path;
        }

        if (file_exists(IN_GAME_PATH . "notifyList.json")) {
            $list = FileUtils::decodeJsonFile(IN_GAME_PATH . "notifyList.json");
            if (!empty($list)) {
                $successful += (int) FileUtils::unlinkFile(IN_GAME_PATH . "notifyList.json");
                foreach ($list as $player => $enabled) {
                    if ($enabled) CloudProvider::current()->enablePlayerNotifications($player);
                }
            }
        }

        if (file_exists(IN_GAME_PATH . "maintenanceList.json")) {
            $list = FileUtils::decodeJsonFile(IN_GAME_PATH . "maintenanceList.json");
            if (!empty($list)) {
                $successful += (int) FileUtils::unlinkFile(IN_GAME_PATH . "maintenanceList.json");
                foreach ($list as $player => $enabled) {
                    if ($enabled) CloudProvider::current()->addToWhitelist($player);
                }
            }
        }

        if (file_exists(IN_GAME_PATH . "modules.json")) {
            $list = FileUtils::decodeJsonFile(IN_GAME_PATH . "modules.json");
            if (!empty($list)) {
                $successful += (int) FileUtils::unlinkFile(IN_GAME_PATH . "modules.json");
                $convertOldName = fn(string $oldModule) => match ($oldModule) {
                    "signModule" => InGameModuleCache::SIGN_MODULE,
                    "npcModule" => InGameModuleCache::NPC_MODULE,
                    "hubCommandModule" => InGameModuleCache::HUB_COMMAND_MODULE,
                    default => null
                };

                foreach ($list as $module => $enabled) {
                    if (($module = $convertOldName($module)) !== null) {
                        QueryBuilder::table(DatabaseTables::MODULES)
                            ->insert([$module => $enabled])
                            ->execute();
                    }
                }
            }
        }

        if (file_exists(TEMPLATES_PATH . "templates.json")) {
            $templatesRaw = FileUtils::decodeJsonFile(TEMPLATES_PATH . "templates.json");
            if (!empty($templatesRaw)) {
                $successful += (int) FileUtils::unlinkFile(TEMPLATES_PATH . "templates.json");
                $templates = [];
                foreach ($templatesRaw as $data) {
                    if (($template = Template::read($data)) !== null) $templates[$template->getName()] = $template;
                }

                foreach ($templates as $template) {
                    CloudProvider::current()->checkTemplate($template->getName())
                        ->then(function (bool $exists) use($template): void {
                            if (!$exists) CloudProvider::current()->addTemplate($template);
                            else CloudLogger::get()->warn("A mysql template with the name §b" . $template->getName() . " §ralready exists, ignoring...");
                        });
                }
            }
        }

        if (file_exists(SERVER_GROUPS_PATH . "groups.json")) {
            $groupsRaw = FileUtils::decodeJsonFile(SERVER_GROUPS_PATH . "groups.json");
            if (!empty($groupsRaw)) {
                $successful += (int) FileUtils::unlinkFile(SERVER_GROUPS_PATH . "groups.json");
                $serverGroups = [];
                foreach ($groupsRaw as $data) {
                    if (($serverGroup = ServerGroup::read($data)) !== null) $serverGroups[$serverGroup->getName()] = $serverGroup;
                }

                foreach ($serverGroups as $serverGroup) {
                    CloudProvider::current()->checkServerGroup($serverGroup->getName())
                        ->then(function (bool $exists) use($serverGroup): void {
                            if (!$exists) CloudProvider::current()->addServerGroup($serverGroup);
                            else CloudLogger::get()->warn("A mysql server group with the name §b" . $serverGroup->getName() . " §ralready exists, ignoring...");
                        });
                }
            }
        }

        return $successful >= count($available);
    }

    public function rollback(): bool {
        $successful = 0;
        $available = [];
        foreach ([TEMPLATES_PATH . "templates.json", SERVER_GROUPS_PATH . "groups.json", IN_GAME_PATH . "maintenanceList.json", IN_GAME_PATH . "modules.json", IN_GAME_PATH . "notifyList.json"] as $path) {
            if (file_exists($path)) $available[] = $path;
        }

        foreach ($available as $path) {
            $successful += (int) FileUtils::copyFile(PathUtils::join($this->currentBackupPath, basename($path)), $path);
        }

        return $successful >= count($available);
    }

    public function id(): string {
        return "migrate-config-json-to-mysql";
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