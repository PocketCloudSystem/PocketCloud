<?php

namespace pocketcloud\command\impl\plugin;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\language\Language;
use pocketcloud\plugin\CloudPluginManager;

class PluginsCommand extends Command {

    public function execute(ICommandSender $sender, string $label, array $args): bool {
        $sender->info("Plugins §8(§e" . count(CloudPluginManager::getInstance()->getPlugins()) . "§8)§r:");
        if (empty(CloudPluginManager::getInstance()->getPlugins())) $sender->info(Language::current()->translate("command.plugins.none"));
        foreach (CloudPluginManager::getInstance()->getPlugins() as $plugin) {
            $sender->info("Name: §e" . $plugin->getDescription()->getName());
            if ($plugin->getDescription()->getDescription() !== null) $sender->info(Language::current()->translate("raw.description") . ": §e" . $plugin->getDescription()->getDescription());
            $sender->info("Version: §ev" . $plugin->getDescription()->getVersion());
            if (!empty($plugin->getDescription()->getAuthors())) $sender->info(Language::current()->translate("raw.author") . ": §e" . implode(", ", $plugin->getDescription()->getAuthors()));
            $sender->info("FullName: §e" . $plugin->getDescription()->getFullName());
            $sender->info(Language::current()->translate("raw.enabled") . ": " . ($plugin->isEnabled() ? "§a" . Language::current()->translate("raw.yes") : "§c" . Language::current()->translate("raw.no")));
        }
        return true;
    }
}