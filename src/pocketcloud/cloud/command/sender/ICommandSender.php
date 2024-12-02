<?php

namespace pocketcloud\cloud\command\sender;

interface ICommandSender {

    public function sendMessage(string $message): void;

    public function getName(): string;
}