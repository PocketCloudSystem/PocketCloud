<?php

namespace pocketcloud\command\impl\server;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\language\Language;
use pocketcloud\network\packet\impl\types\CommandExecutionResult;
use pocketcloud\server\CloudServerManager;

class ExecuteCommand extends Command {

    public function execute(ICommandSender $sender, string $label, array $args): bool {
        if (isset($args[0]) && isset($args[1])) {
            if (($server = CloudServerManager::getInstance()->getServerByName(array_shift($args))) !== null) {
                $sender->info(Language::current()->translate("command.execute", $server->getName()));
                CloudServerManager::getInstance()->sendCommand($server, implode(" ", $args))->then(function(CommandExecutionResult $result) use($server, $sender): void {
                    $server->getCloudServerStorage()->remove("command_promise")->remove("command_promise_time");
                    $sender->info(Language::current()->translate("command.execute.success", $server->getName()));
                    if (empty($result->getMessages())) $sender->info("§c/");
                    else foreach ($result->getMessages() as $message) $sender->info("§e" . $server->getName() . "§8: §r" . $message);
                })->failure(function() use($server, $sender): void {
                    $server->getCloudServerStorage()->remove("command_promise")->remove("command_promise_time");
                    $sender->info(Language::current()->translate("command.execute.failed", $server->getName()));
                });
            } else $sender->error(Language::current()->translate("server.not.found"));
        } else return false;
        return true;
    }
}