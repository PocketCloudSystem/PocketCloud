<?php

namespace pocketcloud\cloud\command\impl\server;

use pocketcloud\cloud\command\argument\def\ServerArgument;
use pocketcloud\cloud\command\argument\def\StringArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\network\packet\impl\type\CommandExecutionResult;
use pocketcloud\cloud\server\CloudServerManager;

class ExecuteCommand extends Command {

    public function __construct() {
        parent::__construct("execute", "Send a command to a server");

        $this->addParameter(new ServerArgument(
            "server",
            false,
            "The server was not found."
        ));

        $this->addParameter(new StringArgument(
            "command",
            false,
            true
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $server = $args["server"];
        $command = $args["command"];

        CloudServerManager::getInstance()->send($server, $command)->then(function(CommandExecutionResult $result) use($server, $sender): void {
            $server->getCloudServerStorage()->remove("command_promise")->remove("command_promise_time");
            $sender->success("The command was successfully handled by the server, response:");
            if (empty($result->getMessages())) $sender->info("§c/");
            else foreach ($result->getMessages() as $message) $sender->info("§b" . $server->getName() . "§8: §r" . $message);
        })->failure(function() use($server, $sender): void {
            $server->getCloudServerStorage()->remove("command_promise")->remove("command_promise_time");
            $sender->error("The command could not be handled by the server.");
        });
        return true;
    }
}