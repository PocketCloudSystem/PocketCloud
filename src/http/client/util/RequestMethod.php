<?php

namespace pocketcloud\cloud\http\client\util;

use UnitEnum;

enum RequestMethod {

    case GET;
    case POST;
    case PATCH;
    case DELETE;
    case PUT;

    public static function fromName(string $name): ?self {
        $name = strtolower($name);
        return array_find(self::cases(), fn(UnitEnum $enum) => strtolower($enum->name) == $name);
    }
}