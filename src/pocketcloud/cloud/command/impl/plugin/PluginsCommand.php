<?php

namespace pocketcloud\cloud\command\impl\plugin;

use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\plugin\CloudPluginManager;

class PluginsCommand extends Command {

    public function __construct() {
        parent::__construct("plugins", "View the current plugins");
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $sender->info("Plugins §8(§b" . count(CloudPluginManager::getInstance()->getAll()) . "§8)§r:");
        if (empty(CloudPluginManager::getInstance()->getAll())) $sender->info("No plugins.");
        foreach (CloudPluginManager::getInstance()->getAll() as $plugin) {
            $sender->info("Name: §b" . $plugin->getDescription()->getName());
            if ($plugin->getDescription()->getDescription() !== null) $sender->info("Description: §b" . $plugin->getDescription()->getDescription());
            $sender->info("Version: §ev" . $plugin->getDescription()->getVersion());
            if (!empty($plugin->getDescription()->getAuthors())) $sender->info("Author(s)=: §b" . implode(", ", $plugin->getDescription()->getAuthors()));
            $sender->info("FullName: §b" . $plugin->getDescription()->getFullName());
            $sender->info("Status: " . ($plugin->isEnabled() ? "§aEnabled" : "§cDisabled"));
        }
        return true;
    }
}