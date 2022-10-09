<?php

namespace pocketcloud\command\impl\general;

use pocketcloud\command\Command;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;
use pocketcloud\template\TemplateType;
use pocketcloud\utils\CloudLogger;

class ListCommand extends Command {

    public function execute(array $args): bool {
        $type = "servers";
        if (isset($args[0])) if (strtolower($args[0]) == "templates" || strtolower($args[0]) == "players" || strtolower($args[0]) == "servers") $type = strtolower($args[0]);

        if ($type == "templates") {
            CloudLogger::get()->info("Templates §8(§e" . count(TemplateManager::getInstance()->getTemplates()) . "§8)§r:");
            if (empty(TemplateManager::getInstance()->getTemplates())) CloudLogger::get()->info("§cNo templates available.");
            foreach (TemplateManager::getInstance()->getTemplates() as $template) {
                CloudLogger::get()->info(
                    "§e" . $template->getName() .
                    " §8- §risLobby: §a" . ($template->isLobby() ? "§aYES" : "§cNO") .
                    " §8- §risMaintenance: §a" . ($template->isMaintenance() ? "§aYES" : "§cNO") .
                    " §8- §rMinServerCount: §e" . $template->getMinServerCount() .
                    " §8- §rMaxServerCount: §e" . $template->getMaxServerCount() .
                    " §8- §risAutoStart: §a" . ($template->isAutoStart() ? "§aYES" : "§cNO") .
                    " §8- §rType: §e" . ($template->getTemplateType() === TemplateType::SERVER() ? "§eSERVER" : "§cPROXY")
                );
            }
        } else if ($type == "servers") {
            CloudLogger::get()->info("Servers §8(§e" . count(CloudServerManager::getInstance()->getServers()) . "§8)§r:");
            if (empty(CloudServerManager::getInstance()->getServers())) CloudLogger::get()->info("§cNo servers available.");
            foreach (CloudServerManager::getInstance()->getServers() as $server) {
                CloudLogger::get()->info(
                    "§e" . $server->getName() .
                    " §8- §rPort: §e" . $server->getCloudServerData()->getPort() . " §8| §rIPv6: §e" . $server->getCloudServerData()->getPort()+1 .
                    " §8- §rTemplate: §e" . $server->getTemplate()->getName() .
                    " §8- §rPlayers: §e" . count($server->getCloudPlayers()) . "§8/§e" . $server->getCloudServerData()->getMaxPlayers() .
                    " §8- §rStatus: §e" . $server->getServerStatus()->getDisplay()
                );
            }
        } else if ($type == "players") {
            CloudLogger::get()->info("Players §8(§e" . count(CloudPlayerManager::getInstance()->getPlayers()) . "§8)§r:");
            if (empty(CloudPlayerManager::getInstance()->getPlayers())) CloudLogger::get()->info("§cNo players are online.");
            foreach (CloudPlayerManager::getInstance()->getPlayers() as $player) {
                CloudLogger::get()->info(
                    "§e" . $player->getName() .
                    " §8- §rHost: §e" . $player->getHost() .
                    " §8- §rXboxUserId: §e" . $player->getXboxUserId() .
                    " §8- §rUniqueId: §e" . $player->getUniqueId() .
                    " §8- §rServer: §e" . ($player->getCurrentServer() === null ? "§cNo server." : $player->getCurrentServer()->getName()) .
                    " §8- §rProxy: §e" . ($player->getCurrentProxy() === null ? "§cNo proxy." : $player->getCurrentProxy()->getName())
                );
            }
        }
        return true;
    }
}