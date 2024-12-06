<?php

namespace pocketcloud\cloud\command\impl\plugin;

use pocketcloud\cloud\command\argument\def\PluginArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\plugin\CloudPluginManager;

class EnableCommand extends Command {

    public function __construct() {
        parent::__construct("enable", "Enable a disabled plugin");

        $this->addParameter(new PluginArgument(
            "plugin",
            false,
            "The plugin was not found."
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $plugin = $args["plugin"];
        CloudPluginManager::getInstance()->enable($plugin);
        return true;
    }
}