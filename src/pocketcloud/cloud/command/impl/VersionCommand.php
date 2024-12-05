<?php

namespace pocketcloud\cloud\command\impl;


use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\util\VersionInfo;

class VersionCommand extends Command {

    public function __construct() {
        parent::__construct("version", "Version information of the current cloud build");
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $sender->info("§7Version: §b" . VersionInfo::VERSION);
        $sender->info("§7Developers: §b" . implode("§8, §b", VersionInfo::DEVELOPERS));
        $sender->info("§7isBeta: §a" . (VersionInfo::BETA ? "§cYES" : "§aNO"));
        return true;
    }
}