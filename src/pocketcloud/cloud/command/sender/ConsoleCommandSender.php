<?php

namespace pocketcloud\cloud\command\sender;

readonly class ConsoleCommandSender implements ICommandSender {


    public function sendMessage(string $message): void {
        //TODO: Logger
    }

    public function getName(): string {
        return "CONSOLE";
    }
}