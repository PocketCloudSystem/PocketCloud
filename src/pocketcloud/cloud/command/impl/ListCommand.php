<?php

namespace pocketcloud\cloud\command\impl;

use pocketcloud\cloud\command\argument\def\StringEnumArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\template\TemplateType;

class ListCommand extends Command {

    public function __construct() {
        parent::__construct("list", "List servers, templates or players");

        $this->addParameter(new StringEnumArgument(
            "type",
            ["servers", "templates", "players"],
            false,
            true
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $type = $args["type"] ?? "servers";

        if ($type == "templates") {
            $sender->info("Templates §8(§b" . count(TemplateManager::getInstance()->getAll()) . "§8)§r:");
            if (empty(TemplateManager::getInstance()->getAll())) $sender->info("§c/");
            foreach (TemplateManager::getInstance()->getAll() as $template) {
                $sender->info(
                    "§b" . $template->getName() .
                    " §8- §risLobby: §a" . ($template->getSettings()->isLobby() ? "§aYes" : "§cNo") .
                    " §8- §risMaintenance: §a" . ($template->getSettings()->isMaintenance() ? "§aYes" : "§cNo") .
                    " §8- §risStatic: §a" . ($template->getSettings()->isStatic() ? "§aYes" : "§cNo") .
                    " §8- §rMinServerCount: §b" . $template->getSettings()->getMinServerCount() .
                    " §8- §rMaxServerCount: §b" . $template->getSettings()->getMaxServerCount() .
                    " §8- §rStartNewPercentage: §b" . $template->getSettings()->getStartNewPercentage() . "%" .
                    " §8- §risAutoStart: §a" . ($template->getSettings()->isAutoStart() ? "§aYes" : "§cNo") .
                    " §8- §rType: §b" . ($template->getTemplateType() === TemplateType::SERVER() ? "§bSERVER" : "§cPROXY")
                );
            }
        } else if ($type == "servers") {
            $sender->info("Servers §8(§b" . count(CloudServerManager::getInstance()->getAll()) . "§8)§r:");
            if (empty(CloudServerManager::getInstance()->getAll())) $sender->info("§c/");
            foreach (CloudServerManager::getInstance()->getAll() as $server) {
                $sender->info(
                    "§b" . $server->getName() .
                    " §8- §rPort: §b" . $server->getCloudServerData()->getPort() . " §8| §rIPv6: §b" . $server->getCloudServerData()->getPort()+1 .
                    " §8- §rTemplate: §b" . $server->getTemplate()->getName() .
                    " §8- §rPlayers: §b" . count($server->getCloudPlayers()) . "§8/§b" . $server->getTemplate()->getSettings()->getMaxPlayerCount() . " §8(§b" . $server->getCloudServerData()->getMaxPlayers() . "§8)" .
                    " §8- §rStatus: §b" . $server->getServerStatus()->getDisplay()
                );
            }
        } else if ($type == "players") {
            $sender->info("Players §8(§b" . count(CloudPlayerManager::getInstance()->getAll()) . "§8)§r:");
            if (empty(CloudPlayerManager::getInstance()->getAll())) $sender->info("§c/");
            foreach (CloudPlayerManager::getInstance()->getAll() as $player) {
                $sender->info(
                    "§b" . $player->getName() .
                    " §8- §rHost: §b" . $player->getHost() .
                    " §8- §rXboxUserId: §b" . $player->getXboxUserId() .
                    " §8- §rUniqueId: §b" . $player->getUniqueId() .
                    " §8- §rServer: §b" . ($player->getCurrentServer() === null ? "§cNo server." : $player->getCurrentServer()->getName()) .
                    " §8- §rProxy: §b" . ($player->getCurrentProxy() === null ? "§cNo proxy." : $player->getCurrentProxy()->getName())
                );
            }
        }
        return true;
    }
}