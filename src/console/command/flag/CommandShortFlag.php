<?php

namespace pocketcloud\cloud\console\command\flag;

final class CommandShortFlag extends CommandFlag {

    public const string PREFIX = "-";
    public const int CHARACTER_LIMIT = 1;

    public function __construct(string $flag, bool $global, bool $expectValue) {
        parent::__construct(self::PREFIX, $flag, self::CHARACTER_LIMIT, $global, $expectValue);
    }

    public static function isLikelyAFlag(string $arg): bool {
        return str_starts_with($arg, self::PREFIX);
    }

    public static function create(string $flag, bool $global = false, bool $expectValue = false): self {
        return new self($flag, $global, $expectValue);
    }
}