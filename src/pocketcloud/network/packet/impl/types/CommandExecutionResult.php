<?php

namespace pocketcloud\network\packet\impl\types;

use pocketcloud\util\Utils;

class CommandExecutionResult {

    public function __construct(
        private string $commandLine,
        private array $messages
    ) {}

    public function getMessage(int $index): ?string {
        return $this->messages[$index] ?? null;
    }

    public function getCommandLine(): string {
        return $this->commandLine;
    }

    public function getMessages(): array {
        return $this->messages;
    }

    public function toArray(): array {
        return [
            "command_line" => $this->commandLine,
            "messages" => $this->messages
        ];
    }

    public static function fromArray(array $data): ?self {
        if (!Utils::containKeys($data, "command_line", "messages")) return null;
        if (is_array($data["messages"])) {
            return new CommandExecutionResult(
                $data["command_line"],
                $data["messages"]
            );
        }
        return null;
    }
}