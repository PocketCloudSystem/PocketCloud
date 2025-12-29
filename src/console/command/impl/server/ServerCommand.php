<?php

namespace pocketcloud\cloud\console\command\impl\server;

use pocketcloud\cloud\console\command\argument\def\IntegerArgument;
use pocketcloud\cloud\console\command\argument\def\MultipleTypesArgument;
use pocketcloud\cloud\console\command\argument\def\ServerArgument;
use pocketcloud\cloud\console\command\argument\def\ServerGroupArgument;
use pocketcloud\cloud\console\command\argument\def\StringEnumArgument;
use pocketcloud\cloud\console\command\argument\def\TemplateArgument;
use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\server\CloudServerManager;

final class ServerCommand extends Command {

    public function __construct() {
        parent::__construct("server", "Manage the cloud's servers");

        $this->registerSubCommand(SubCommand::fromClosure("start", function (ICommandSender $sender, string $label, array $args): bool {
            $template = $args["template"];
            $amount = $args["amount"] ?? 0;
            CloudServerManager::getInstance()->start($template, $amount);
            return true;
        }, false)->addParameter(new TemplateArgument("template", false))->addParameter(new IntegerArgument("amount", true, function (int $number): int {
            if ($number < 0 || $number > 20) return 1;
            return $number;
        })));

        $this->registerSubCommand(SubCommand::fromClosure("stop", function (ICommandSender $sender, string $label, array $args): bool {
            $server = $args["server"];

            if (is_string($server) && $server == "all") {
                CloudServerManager::getInstance()->stopAll(true);
            } else {
                CloudServerManager::getInstance()->stop($server, true);
            }
            return true;
        }, false)->addParameter(new MultipleTypesArgument("server", [
            new ServerArgument("server", false),
            new TemplateArgument("template", false),
            new ServerGroupArgument("server", false),
            new StringEnumArgument("all", ["all"], false, false)
        ], false)));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        return true;
    }
}