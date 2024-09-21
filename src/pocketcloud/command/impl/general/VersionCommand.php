<?php

namespace pocketcloud\command\impl\general;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\util\VersionInfo;

class VersionCommand extends Command {
    public function execute(ICommandSender $sender, string $label, array $args): bool {
        $sender->info("§7Version: §e" . VersionInfo::VERSION);
        $sender->info("§7Developers: §e" . implode("§8, §e", VersionInfo::DEVELOPERS));
        $sender->info("§7isBeta: §a" . (VersionInfo::BETA ? "§cYES" : "§aNO"));
        return true;
    }
}