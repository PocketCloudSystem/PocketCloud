<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\util\VersionInfo;

final class VersionCommand extends Command {

    public function __construct() {
        parent::__construct("version", "Version information of the current cloud build", ["v", "ver"]);
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        $sender->info("§7Version: §b{}", VersionInfo::VERSION);
        $sender->info("§7Developers: §b{}", implode("§8, §b", VersionInfo::DEVELOPERS));
        $sender->info("§7Beta: §a{}", VersionInfo::BETA ? "§cYES" : "§aNO");
        return true;
    }
}