<?php

namespace pocketcloud\command\impl\plugin;

use pocketcloud\command\Command;
use pocketcloud\plugin\PluginManager;
use pocketcloud\utils\CloudLogger;

class PluginsCommand extends Command {

    public function execute(array $args): bool {
        CloudLogger::get()->info("Plugins §8(§e" . count(PluginManager::getInstance()->getPlugins()) . "§8)§r:");
        if (empty(PluginManager::getInstance()->getPlugins())) CloudLogger::get()->info("§cNo plugins available.");
        foreach (PluginManager::getInstance()->getPlugins() as $plugin) {
            CloudLogger::get()->info("Name: §e" . $plugin->getDescription()->getName());
            if ($plugin->getDescription()->getDescription() !== null) CloudLogger::get()->info("Description: §e" . $plugin->getDescription()->getDescription());
            CloudLogger::get()->info("Version: §ev" . $plugin->getDescription()->getVersion());
            if (!empty($plugin->getDescription()->getAuthors())) CloudLogger::get()->info("Author" . (count($plugin->getDescription()->getAuthors()) == 1 ? "" : "s") . ": §e" . implode(", ", $plugin->getDescription()->getAuthors()));
            CloudLogger::get()->info("FullName: §e" . $plugin->getDescription()->getFullName());
            CloudLogger::get()->info("Status: §a" . ($plugin->isEnabled() ? "§aEnabled" : "§cDisabled"));
        }
        return true;
    }
}