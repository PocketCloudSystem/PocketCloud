<?php

namespace pocketcloud\cloud\command\impl\plugin;

use pocketcloud\cloud\command\argument\def\PluginArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\plugin\CloudPluginManager;

class DisableCommand extends Command {

    public function __construct() {
        parent::__construct("disable", "Disable a running plugin");

        $this->addParameter(new PluginArgument(
            "plugin",
            false
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $plugin = $args["plugin"];
        CloudPluginManager::getInstance()->disable($plugin);
        return true;
    }
}