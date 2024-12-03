<?php

namespace pocketcloud\cloud\provider\migration;

final class JsonToMySqlMigrator implements IMigrator {

    public function checkForMigration(): bool {
        return file_exists(TEMPLATES_PATH . "templates.json") ||
            file_exists(IN_GAME_PATH . "maintenanceList.json") ||
            file_exists(IN_GAME_PATH . "modules.json") ||
            file_exists(IN_GAME_PATH . "notifyList.json");
    }

    public function migrate(): bool {
        return false;
    }
}