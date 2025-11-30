<?php

namespace pocketcloud\cloud\command\impl;

use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\setup\impl\ConfigSetup;

final class ConfigureCommand extends Command {

    public function __construct() {
        parent::__construct("configure", "Reconfigure the config");
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        new ConfigSetup()->startSetup();
        return true;
    }
}