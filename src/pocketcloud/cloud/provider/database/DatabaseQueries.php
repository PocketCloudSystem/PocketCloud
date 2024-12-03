<?php

namespace pocketcloud\cloud\provider\database;

use pocketcloud\cloud\util\enum\ParameterEnumTrait;
use r3pt1s\mysql\query\MySQLQuery;

/**
* @method static MySQLQuery addTemplate(string $name)
 */
final class DatabaseQueries {
    use ParameterEnumTrait;

    protected static function init(): void {
        self::register("addTemplate", function (string $name): ?MySQLQuery {
            var_dump($name);
            return null;
        });
    }
}