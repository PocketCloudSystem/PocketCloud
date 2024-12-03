<?php

namespace pocketcloud\cloud\provider\migration;

use pocketcloud\cloud\util\enum\EnumTrait;

/**
 * @method static JsonToMySqlMigrator JSON_TO_MYSQL()
 */
final class MigrationList {
    use EnumTrait;

    protected static function init(): void {
        self::register("json_to_mysql", new JsonToMySqlMigrator());
    }
}