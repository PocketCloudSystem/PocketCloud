<?php

namespace pocketcloud\cloud\provider\migration;

use pocketcloud\cloud\util\trait\RegistryTrait;

/**
 * @method static JsonToMySqlMigrator JSON_TO_MYSQL()
 * @method static JsonToMySqlMigrator V3_TO_V4()
 */
final class MigrationList {
    use RegistryTrait;

    protected static function init(): void {
        self::register("json_to_mysql", new JsonToMySqlMigrator());
        self::register("v3_to_v4", new V3ToV4Migrator());
    }
}