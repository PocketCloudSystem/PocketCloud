<?php

namespace pocketcloud\cloud\console\command\argument\def;

use Closure;
use pocketcloud\cloud\console\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\console\command\argument\CommandArgument;

readonly class IntegerArgument extends CommandArgument {

    /**
     * @param string $name
     * @param bool $optional
     * @param Closure(int $number): int|null $integerProcessClosure Pre-process the integer before it is getting passed to the arguments
     * @param string|null $customErrorMessage
     */
    public function __construct(
        string $name,
        bool $optional,
        private ?Closure $integerProcessClosure = null,
        ?string $customErrorMessage = null
    ) {
        parent::__construct($name, $optional, $customErrorMessage);
    }

    public function parseValue(string $input): int {
        if (is_numeric($input)) {
            if ($this->integerProcessClosure !== null) return ($this->integerProcessClosure)(intval($input));
            return intval($input);
        }
        return throw new ArgumentParseException();
    }

    public function getType(): string {
        return "integer";
    }
}