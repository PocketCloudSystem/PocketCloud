<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\console\screen\impl\MonitorScreen;
use pocketcloud\cloud\console\screen\ScreenManager;

final class MonitorCommand extends Command {

    public function __construct() {
        parent::__construct("monitor", "Monitor the cloud's and the servers' performance stats");
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        ScreenManager::getInstance()->setCurrentScreen(new MonitorScreen());
        return true;
    }
}