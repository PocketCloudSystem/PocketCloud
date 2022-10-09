<?php

namespace pocketcloud\command\impl\plugin;

use pocketcloud\command\Command;
use pocketcloud\plugin\PluginManager;
use pocketcloud\utils\CloudLogger;

class EnableCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0])) {
            if (($plugin = PluginManager::getInstance()->getPluginByName($args[0])) !== null) {
                if ($plugin->isDisabled()) {
                    PluginManager::getInstance()->enablePlugin($plugin);
                } else CloudLogger::get()->error("§cThe plugin isn't disabled!");
            } else CloudLogger::get()->error("§cThe plugin doesn't exists!");
        } else return false;
        return true;
    }
}