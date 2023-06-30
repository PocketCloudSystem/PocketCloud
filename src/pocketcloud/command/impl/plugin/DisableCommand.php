<?php

namespace pocketcloud\command\impl\plugin;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\language\Language;
use pocketcloud\plugin\CloudPluginManager;

class DisableCommand extends Command {

    public function execute(ICommandSender $sender, string $label, array $args): bool {
        if (isset($args[0])) {
            if (($plugin = CloudPluginManager::getInstance()->getPluginByName($args[0])) !== null) {
                if ($plugin->isEnabled()) {
                    CloudPluginManager::getInstance()->disablePlugin($plugin);
                } else $sender->error(Language::current()->translate("command.disable.failed"));
            } else $sender->error(Language::current()->translate("plugin.not.found"));
        } else return false;
        return true;
    }
}