<?php

namespace pocketcloud\command\impl\plugin;

use pocketcloud\command\Command;
use pocketcloud\language\Language;
use pocketcloud\plugin\CloudPluginManager;
use pocketcloud\util\CloudLogger;

class PluginsCommand extends Command {

    public function execute(string $label, array $args): bool {
        CloudLogger::get()->info("Plugins §8(§e" . count(CloudPluginManager::getInstance()->getPlugins()) . "§8)§r:");
        if (empty(CloudPluginManager::getInstance()->getPlugins())) CloudLogger::get()->info(Language::current()->translate("command.plugins.none"));
        foreach (CloudPluginManager::getInstance()->getPlugins() as $plugin) {
            CloudLogger::get()->info("Name: §e" . $plugin->getDescription()->getName());
            if ($plugin->getDescription()->getDescription() !== null) CloudLogger::get()->info(Language::current()->translate("raw.description") . ": §e" . $plugin->getDescription()->getDescription());
            CloudLogger::get()->info("Version: §ev" . $plugin->getDescription()->getVersion());
            if (!empty($plugin->getDescription()->getAuthors())) CloudLogger::get()->info(Language::current()->translate("raw.author") . ": §e" . implode(", ", $plugin->getDescription()->getAuthors()));
            CloudLogger::get()->info("FullName: §e" . $plugin->getDescription()->getFullName());
            CloudLogger::get()->info(Language::current()->translate("raw.enabled") . ": " . ($plugin->isEnabled() ? "§a" . Language::current()->translate("raw.yes") : "§c" . Language::current()->translate("raw.no")));
        }
        return true;
    }
}