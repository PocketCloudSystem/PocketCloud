<?php

namespace pocketcloud\command\impl\plugin;

use pocketcloud\command\Command;
use pocketcloud\plugin\PluginManager;
use pocketcloud\utils\CloudLogger;

class DisableCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0])) {
            if (($plugin = PluginManager::getInstance()->getPluginByName($args[0])) !== null) {
                if ($plugin->isEnabled()) {
                    PluginManager::getInstance()->disablePlugin($plugin);
                } else CloudLogger::get()->error("§cThe plugin isn't enabled!");
            } else CloudLogger::get()->error("§cThe plugin doesn't exists!");
        } else return false;
        return true;
    }
}