<?php

namespace pocketcloud\cloud\command\argument;

use pocketcloud\cloud\command\argument\exception\ArgumentParseException;

interface IArgument {

    public function getName(): string;

    /** @throws ArgumentParseException */
    public function parseValue(string $input): mixed;
}