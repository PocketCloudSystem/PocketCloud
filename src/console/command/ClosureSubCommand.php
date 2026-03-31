<?php

namespace pocketcloud\cloud\console\command;

use Closure;
use pocketcloud\cloud\console\command\sender\ICommandSender;

final class ClosureSubCommand extends SubCommand {

    /**
     * @param string $name
     * @param Closure(ICommandSender $sender, string $label, array $args, array $flags): bool|null $executeHandler
     * @param array $standaloneAliases
     * @param string|null $usage
     */
    public function __construct(
        string $name,
        private readonly ?Closure $executeHandler,
        array $standaloneAliases = [],
        ?string $usage = null
    ) {
        parent::__construct($name, $standaloneAliases, $usage);
    }

    public function run(ICommandSender $sender, string $label, array $args, array $flags): bool {
        if ($this->executeHandler === null) return true;
        return ($this->executeHandler)($sender, $label, $args, $flags);
    }
}