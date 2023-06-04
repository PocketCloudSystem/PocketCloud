<?php

namespace pocketcloud\command\impl\server;

use pocketcloud\command\Command;
use pocketcloud\language\Language;
use pocketcloud\network\packet\impl\types\CommandExecutionResult;
use pocketcloud\server\CloudServerManager;
use pocketcloud\util\CloudLogger;

class ExecuteCommand extends Command {

    public function execute(string $label, array $args): bool {
        if (isset($args[0]) && isset($args[1])) {
            if (($server = CloudServerManager::getInstance()->getServerByName(array_shift($args))) !== null) {
                CloudLogger::get()->info(Language::current()->translate("command.execute", $server->getName()));
                CloudServerManager::getInstance()->sendCommand($server, implode(" ", $args))->then(function(CommandExecutionResult $result) use($server): void {
                    $server->getCloudServerStorage()->remove("command_promise")->remove("command_promise_time");
                    CloudLogger::get()->info(Language::current()->translate("command.execute.success", $server->getName()));
                    if (empty($result->getMessages())) CloudLogger::get()->info("§c/");
                    else foreach ($result->getMessages() as $message) CloudLogger::get()->info("§e" . $server->getName() . "§8: §r" . $message);
                })->failure(function() use($server): void {
                    $server->getCloudServerStorage()->remove("command_promise")->remove("command_promise_time");
                    CloudLogger::get()->info(Language::current()->translate("command.execute.failed", $server->getName()));
                });
            } else CloudLogger::get()->error(Language::current()->translate("server.not.found"));
        } else return false;
        return true;
    }
}