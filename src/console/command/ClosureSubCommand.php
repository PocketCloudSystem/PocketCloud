<?php

namespace pocketcloud\cloud\console\command;

use Closure;
use pocketcloud\cloud\console\command\sender\ICommandSender;

final class ClosureSubCommand extends SubCommand {

    /**
     * @param string $name
     * @param Closure(ICommandSender $sender, string $label, array $args): bool|null $executeHandler
     * @param string|null $usage
     * @param array $aliases
     */
    public function __construct(
        string $name,
        private readonly ?Closure $executeHandler,
        ?string $usage = null,
        array $aliases = []
    ) {
        parent::__construct($name, $usage, $aliases);
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        if ($this->executeHandler === null) return true;
        return ($this->executeHandler)($sender, $label, $args);
    }
}