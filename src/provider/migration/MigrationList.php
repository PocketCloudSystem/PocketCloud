<?php

namespace pocketcloud\cloud\provider\migration;

use pocketcloud\cloud\util\trait\RegistryTrait;

/**
 * @method static JsonToMySqlMigrator JSON_TO_MYSQL()
 */
final class MigrationList {
    use RegistryTrait;

    protected static function init(): void {
        self::register("json_to_mysql", new JsonToMySqlMigrator());
    }
}