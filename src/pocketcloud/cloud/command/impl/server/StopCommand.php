<?php

namespace pocketcloud\cloud\command\impl\server;

use pocketcloud\cloud\command\argument\def\MultipleTypesArgument;
use pocketcloud\cloud\command\argument\def\ServerArgument;
use pocketcloud\cloud\command\argument\def\StringEnumArgument;
use pocketcloud\cloud\command\argument\def\TemplateArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\server\CloudServerManager;

class StopCommand extends Command {

    public function __construct() {
        parent::__construct("stop", "Stop a server");

        $this->addParameter(new MultipleTypesArgument(
            "object",
            [
                new ServerArgument("server", false),
                new TemplateArgument("template", false),
                new StringEnumArgument("all", ["all"], false, false)
            ],
            false
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $object = $args["object"];

        if (!($object == "all" ? CloudServerManager::getInstance()->stopAll() : CloudServerManager::getInstance()->stop($object))) {
            $sender->warn("The server was not found!");
        }
        return true;
    }
}