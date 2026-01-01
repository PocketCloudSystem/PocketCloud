<?php

namespace pocketcloud\cloud\network\packet\data;

use pocketcloud\cloud\util\Utils;

readonly final class ServerCommandExecutionResult {

    public function __construct(
        private string $id,
        private string $commandLine,
        private array $messages
    ) {}

    public function getId(): string {
        return $this->id;
    }

    public function getMessage(int $index): ?string {
        return $this->messages[$index] ?? null;
    }

    public function getCommandLine(): string {
        return $this->commandLine;
    }

    public function getMessages(): array {
        return $this->messages;
    }

    public function write(): array {
        return [
            "id" => $this->id,
            "command_line" => $this->commandLine,
            "messages" => $this->messages
        ];
    }

    public static function read(array $data): ?self {
        if (!Utils::containKeys($data, "id", "command_line", "messages")) return null;
        if (is_array($data["messages"])) return new self($data["id"], $data["command_line"], $data["messages"]);
        return null;
    }
}