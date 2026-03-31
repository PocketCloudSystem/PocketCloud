<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\console\log\logger\cache\LogMessagesCache;
use pocketcloud\cloud\util\TerminalUtils;

final class ClearCommand extends Command {

    public function __construct() {
        parent::__construct("clear", "Clears the console", ["cls", "purge"]);
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand, array $flags): bool {
        LogMessagesCache::clear();
        TerminalUtils::clearConsole();
        return true;
    }
}