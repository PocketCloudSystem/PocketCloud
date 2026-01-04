<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\parameter\def\CommandNameParameter;
use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\CommandManager;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;

final class HelpCommand extends Command {

    public function __construct() {
        parent::__construct("help", "List all commands", ["?"]);
        $this->addParameter(new CommandNameParameter(
            "command",
            true
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        $originCommand = $args["command"] ?? null;
        $commands = $originCommand === null ? CommandManager::getInstance()->getAll() : [$originCommand];

        foreach ($commands as $command) {
            $usages = explode("\n", $command->getUsage());
            $extraPrint = count($usages) > 1;
            $sender->info("§b" . $command->getName() .
                " §8- §r" . $command->getDescription() .
                ($extraPrint || $originCommand === null ? "" : " §8- §r" . current($usages)) .
                " §8- §r[§c" . (empty($command->getAliases()) ? "" : implode("§8, §c", $command->getAliases())) . "§r]"
            );

            if ($extraPrint && $originCommand !== null) {
                foreach ($usages as $usage) {
                    $sender->info("§8» §r" . $usage);
                }
            }
        }
        return true;
    }
}