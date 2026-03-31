<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\cache\MaintenanceListCache;
use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\ITabComplete;
use pocketcloud\cloud\console\command\parameter\def\StringParameter;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\provider\CloudProvider;

final class MaintenanceCommand extends Command implements ITabComplete {

    public function __construct() {
        parent::__construct("maintenance", "Manage the whitelist", ["whitelist", "mnt"]);

        $this->registerSubCommand(SubCommand::fromClosure("add", $this->handleAddSub(...))
            ->addParameter(new StringParameter("player", false, true)));

        $this->registerSubCommand(SubCommand::fromClosure("remove", $this->handleRemoveSub(...))
            ->addParameter(new StringParameter("player", false, true)));

        $this->registerSubCommand(SubCommand::fromClosure("list", $this->handleListSub(...)));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand, array $flags): bool {
        return true;
    }

    public function handleAddSub(ICommandSender $sender, string $label, array $args, array $flags): bool {
        $player = $args["player"];
        if (!MaintenanceListCache::is($player)) {
            $sender->success("Successfully §aadded §rthe player to the maintenance list!");
            CloudProvider::current()->addToWhitelist($player);
        } else $sender->warn("The player is already on the maintenance list!");
        return true;
    }

    public function handleRemoveSub(ICommandSender $sender, string $label, array $args, array $flags): bool {
        $player = $args["player"];
        if (MaintenanceListCache::is($player)) {
            $sender->success("Successfully §cremoved §rthe player from the maintenance list!");
            CloudProvider::current()->removeFromWhitelist($player);
        } else $sender->warn("The player is not on the maintenance list!");
        return true;
    }

    public function handleListSub(ICommandSender $sender, string $label, array $args, array $flags): bool {
        $list = MaintenanceListCache::getAll();
        $sender->info("Players: §8(§b" . count($list) . "§8)");
        if (empty($list)) $sender->info("§cNo players on the maintenance list");
        else $sender->info("§b" . implode("§8, §b", $list));
        return true;
    }

    public function onTabComplete(array $args): array {
        if (count($args) == 2) {
            if ($args[0] == "remove") {
                return MaintenanceListCache::getAll();
            }
        }

        return [];
    }
}