<?php

namespace pocketcloud\command\impl\template;

use pocketcloud\command\Command;
use pocketcloud\config\MaintenanceConfig;
use pocketcloud\utils\CloudLogger;

class MaintenanceCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0])) {
            if (strtolower($args[0]) == "add") {
                if (isset($args[1])) {
                    array_shift($args);
                    $target = trim(implode(" ", $args));
                    if (!MaintenanceConfig::getInstance()->is($target)) {
                        CloudLogger::get()->info("Successfully added the player §e" . $target . " §rto the maintenance list!");
                        MaintenanceConfig::getInstance()->add($target);
                    } else CloudLogger::get()->error("§cThe player is already on the maintenance list!");
                } else return false;
            } else if (strtolower($args[0]) == "remove") {
                if (isset($args[1])) {
                    array_shift($args);
                    $target = trim(implode(" ", $args));
                    if (MaintenanceConfig::getInstance()->is($target)) {
                        CloudLogger::get()->info("Successfully removed the player §e" . $target . " §rfrom the maintenance list!");
                        MaintenanceConfig::getInstance()->remove($target);
                    } else CloudLogger::get()->error("§cThe player isn't on the maintenance list!");
                } else return false;
            } else if (strtolower($args[0]) == "list") {
                $players = MaintenanceConfig::getInstance()->getConfig()->getAll(true);
                CloudLogger::get()->info("Players: §8(§e" . count($players) . "§8)");
                if (empty($players)) CloudLogger::get()->info("§cThere are no players.");
                else CloudLogger::get()->info("§e" . implode("§8, §e", $players));
            } else return false;
        } else return false;
        return true;
    }
}