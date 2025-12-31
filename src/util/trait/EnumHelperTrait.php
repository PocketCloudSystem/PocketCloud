<?php

namespace pocketcloud\cloud\util\trait;

use UnitEnum;

trait EnumHelperTrait {

    public static function fromName(string $name): ?self {
        return array_find(self::cases(), fn(UnitEnum $case) => $case->name == $name);
    }
}