<?php

namespace pocketcloud\command\impl\plugin;

use pocketcloud\command\Command;
use pocketcloud\language\Language;
use pocketcloud\plugin\CloudPluginManager;
use pocketcloud\util\CloudLogger;

class EnableCommand extends Command {

    public function execute(string $label, array $args): bool {
        if (isset($args[0])) {
            if (($plugin = CloudPluginManager::getInstance()->getPluginByName($args[0])) !== null) {
                if ($plugin->isDisabled()) {
                    CloudPluginManager::getInstance()->enablePlugin($plugin);
                } else CloudLogger::get()->error(Language::current()->translate("command.enable.failed"));
            } else CloudLogger::get()->error(Language::current()->translate("plugin.not.found"));
        } else return false;
        return true;
    }
}