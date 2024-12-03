<?php

namespace pocketcloud\cloud\provider\database;

use pocketcloud\cloud\template\TemplateHelper;
use pocketcloud\cloud\util\enum\ParameterEnumTrait;
use r3pt1s\mysql\query\QueryBuilder;

/**
 * @method static QueryBuilder createTables()
 * @method static QueryBuilder addTemplate(array $data)
 * @method static QueryBuilder removeTemplate(string $name)
 * @method static QueryBuilder editTemplate(string $name, array $newData)
 * @method static QueryBuilder getTemplate(string $name)
 * @method static QueryBuilder checkTemplate(string $name)
 * @method static QueryBuilder getTemplates()
 */
final class DatabaseQueries {
    use ParameterEnumTrait;

    protected static function init(): void {
        self::register("createTables", function (): ?QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->create([
                    "name" => "VARCHAR(50) PRIMARY KEY",
                    "lobby" => "BOOL",
                    "maintenance" => "BOOL",
                    "static" => "BOOL",
                    "maxPlayerCount" => "INTEGER",
                    "minServerCount" => "INTEGER",
                    "maxServerCount" => "INTEGER",
                    "startNewPercentage" => "INTEGER",
                    "autoStart" => "BOOL",
                    "templateType" => "VARCHAR(10)"
                ])
                ->changeTable(DatabaseTables::MODULES)
                    ->create([
                        "module" => "VARCHAR(100)",
                        "enabled" => "BOOL"
                    ])
                ->changeTable(DatabaseTables::NOTIFICATIONS)
                    ->create([
                        "player" => "VARCHAR(16) PRIMARY KEY"
                    ])
                ->changeTable(DatabaseTables::MAINTENANCE_LIST)
                    ->create([
                        "player" => "VARCHAR(16) PRIMARY KEY"
                    ]);
        });

        self::register("addTemplate", function (array $data): ?QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->insert($data);
        });

        self::register("removeTemplate", function (string $name): ?QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->delete(["name" => $name]);
        });

        self::register("editTemplate", function (string $name, array $newData): ?QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->update($newData, ["name" => $name]);
        });

        self::register("getTemplate", function (string $name): ?QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->get(TemplateHelper::KEYS, ["name" => $name]);
        });

        self::register("checkTemplate", function (string $name): ?QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->has(["name" => $name]);
        });

        self::register("getTemplates", function (): ?QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->select(TemplateHelper::KEYS, "*");
        });
    }
}