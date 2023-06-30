<?php

namespace pocketcloud\command\impl\template;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\config\MaintenanceList;
use pocketcloud\language\Language;

class MaintenanceCommand extends Command {

    public function execute(ICommandSender $sender, string $label, array $args): bool {
        if (isset($args[0])) {
            if (strtolower($args[0]) == "add") {
                if (isset($args[1])) {
                    array_shift($args);
                    $target = trim(implode(" ", $args));
                    if (!MaintenanceList::is($target)) {
                        $sender->info(Language::current()->translate("command.maintenance.success.first", $target));
                        MaintenanceList::add($target);
                    } else $sender->error(Language::current()->translate("command.maintenance.failed.first"));
                } else return false;
            } else if (strtolower($args[0]) == "remove") {
                if (isset($args[1])) {
                    array_shift($args);
                    $target = trim(implode(" ", $args));
                    if (MaintenanceList::is($target)) {
                        $sender->info(Language::current()->translate("command.maintenance.success.second", $target));
                        MaintenanceList::remove($target);
                    } else $sender->error(Language::current()->translate("command.maintenance.failed.second"));
                } else return false;
            } else if (strtolower($args[0]) == "list") {
                $players = MaintenanceList::all();
                $sender->info("Players: §8(§e" . count($players) . "§8)");
                if (empty($players)) $sender->info(Language::current()->translate("command.maintenance.failed.third"));
                else $sender->info("§e" . implode("§8, §e", $players));
            } else return false;
        } else return false;
        return true;
    }
}