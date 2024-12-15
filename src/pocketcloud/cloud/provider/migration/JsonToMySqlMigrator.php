<?php

namespace pocketcloud\cloud\provider\migration;

use pocketcloud\cloud\cache\InGameModule;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\provider\database\DatabaseTables;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\FileUtils;
use r3pt1s\mysql\query\QueryBuilder;

final class JsonToMySqlMigrator implements IMigrator {

    public function checkForMigration(): bool {
        return file_exists(TEMPLATES_PATH . "templates.json") ||
            file_exists(SERVER_GROUPS_PATH . "groups.json") ||
            file_exists(IN_GAME_PATH . "maintenanceList.json") ||
            file_exists(IN_GAME_PATH . "modules.json") ||
            file_exists(IN_GAME_PATH . "notifyList.json");
    }

    public function migrate(): bool {
        if (!file_exists($backupPath = STORAGE_PATH . "backups/")) mkdir(STORAGE_PATH . "backups/");

        if (file_exists(IN_GAME_PATH . "notifyList.json")) {
            $list = FileUtils::jsonDecode(FileUtils::fileGetContents(IN_GAME_PATH . "notifyList.json"));
            if (!empty($list)) {
                FileUtils::copyFile(IN_GAME_PATH . "notifyList.json", $backupPath . "notifyList.json");
                FileUtils::unlinkFile(IN_GAME_PATH . "notifyList.json");
                foreach ($list as $player => $enabled) {
                    if ($enabled) CloudProvider::current()->enablePlayerNotifications($player);
                }
            }
        }

        if (file_exists(IN_GAME_PATH . "maintenanceList.json")) {
            $list = FileUtils::jsonDecode(FileUtils::fileGetContents(IN_GAME_PATH . "maintenanceList.json"));
            if (!empty($list)) {
                FileUtils::copyFile(IN_GAME_PATH . "maintenanceList.json", $backupPath . "maintenanceList.json");
                FileUtils::unlinkFile(IN_GAME_PATH . "maintenanceList.json");
                foreach ($list as $player => $enabled) {
                    if ($enabled) CloudProvider::current()->addToWhitelist($player);
                }
            }
        }

        if (file_exists(IN_GAME_PATH . "modules.json")) {
            $list = FileUtils::jsonDecode(FileUtils::fileGetContents(IN_GAME_PATH . "modules.json"));
            if (!empty($list)) {
                FileUtils::copyFile(IN_GAME_PATH . "modules.json", $backupPath . "modules.json");
                FileUtils::unlinkFile(IN_GAME_PATH . "modules.json");
                $convertOldName = fn(string $oldModule) => match ($oldModule) {
                    "signModule" => InGameModule::SIGN_MODULE,
                    "npcModule" => InGameModule::NPC_MODULE,
                    "hubCommandModule" => InGameModule::HUB_COMMAND_MODULE,
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
            $templatesRaw = FileUtils::jsonDecode(FileUtils::fileGetContents(TEMPLATES_PATH . "templates.json"));
            if (!empty($templatesRaw)) {
                FileUtils::copyFile(TEMPLATES_PATH . "templates.json", $backupPath . "templates.json");
                FileUtils::unlinkFile(TEMPLATES_PATH . "templates.json");
                $templates = [];
                foreach ($templatesRaw as $data) {
                    if (($template = Template::fromArray($data)) !== null) $templates[$template->getName()] = $template;
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
            $groupsRaw = FileUtils::jsonDecode(FileUtils::fileGetContents(SERVER_GROUPS_PATH . "groups.json"));
            if (!empty($groupsRaw)) {
                FileUtils::copyFile(SERVER_GROUPS_PATH . "groups.json", SERVER_GROUPS_PATH . "groups.json");
                FileUtils::unlinkFile(SERVER_GROUPS_PATH . "groups.json");
                $serverGroups = [];
                foreach ($groupsRaw as $data) {
                    if (($serverGroup = ServerGroup::fromArray($data)) !== null) $serverGroups[$serverGroup->getName()] = $serverGroup;
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
        return true;
    }
}