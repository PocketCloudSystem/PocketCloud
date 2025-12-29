<?php

namespace pocketcloud\cloud\console\command;

use Closure;
use pocketcloud\cloud\console\command\sender\ICommandSender;

final class ClosureSubCommand extends SubCommand {

    public function __construct(
        string $name,
        private readonly ?Closure $executeHandler,
        ?string $description = null,
        ?string $usage = null,
        array $aliases = []
    ) {
        parent::__construct($name, $description, $usage, $aliases);
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        if ($this->executeHandler === null) return true;
        return ($this->executeHandler)($sender, $label, $args);
    }
}