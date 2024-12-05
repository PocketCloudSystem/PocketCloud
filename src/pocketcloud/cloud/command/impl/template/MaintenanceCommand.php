<?php

namespace pocketcloud\cloud\command\impl\template;

use pocketcloud\cloud\command\argument\def\StringArgument;
use pocketcloud\cloud\command\argument\def\StringEnumArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\provider\CloudProvider;

class MaintenanceCommand extends Command {

    public function __construct() {
        parent::__construct("maintenance", "Manage the maintenance list");

        $this->addParameter(new StringEnumArgument(
            "action",
            ["add", "remove", "list"],
            false,
            false
        ));

        $this->addParameter(new StringArgument(
            "player",
            true,
            true
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $action = $args["action"];
        $player = $args["player"] ?? null;

        switch ($action) {
            case "add": {
                if ($player === null) return false;
                CloudProvider::current()->isOnWhitelist($player)
                    ->then(function (bool $isOnWhitelist) use($player, $sender): void {
                        if (!$isOnWhitelist) {
                            $sender->info("Successfully §aadded §rthe player to the maintenance list!");
                            CloudProvider::current()->addToWhitelist($player);
                        } else $sender->warn("The player is already on the maintenance list!");
                    })->failure(fn() => $sender->warn("Something unexpected happened while retrieving the maintenance list..."));
                break;
            }
            case "remove": {
                if ($player === null) return false;
                CloudProvider::current()->isOnWhitelist($player)
                    ->then(function (bool $isOnWhitelist) use($player, $sender): void {
                        if ($isOnWhitelist) {
                            $sender->info("Successfully §cremoved §rthe player to the maintenance list!");
                            CloudProvider::current()->removeFromWhitelist($player);
                        } else $sender->warn("The player is not on the maintenance list!");
                    })->failure(fn() => $sender->warn("Something unexpected happened while retrieving the maintenance list..."));
                break;
            }
            case "list": {
                CloudProvider::current()->getWhitelist()
                    ->then(function (array $list) use($sender): void {
                        $sender->info("Players: §8(§b" . count($list) . "§8)");
                        if (empty($list)) $sender->info("§cNo players on the maintenance list");
                        else $sender->info("§b" . implode("§8, §b", $list));
                    })->failure(fn() => $sender->warn("Something unexpected happened while retrieving the maintenance list..."));
                break;
            }
            default: {
                return false;
            }
        }
        return true;
    }
}