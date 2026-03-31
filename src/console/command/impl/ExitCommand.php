<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\flag\CommandFlag;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\PocketCloud;

final class ExitCommand extends Command {

    public function __construct() {
        parent::__construct("exit", "Stop the cloud");
        $this->addFlag(CommandFlag::short("y"));
        $this->addFlag(CommandFlag::long("yes"));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand, array $flags): bool {
        if (($flags["y"] ?? false) || ($flags["yes"] ?? false)) PocketCloud::getInstance()->shutdown();
        else $this->waitForConfirmation($sender, "§bAre you sure you want to stop the cloud?", ["yes", "true", "y", "t"])->then(function (bool $response): void {
            if ($response) PocketCloud::getInstance()->shutdown();
        });

        return true;
    }
}