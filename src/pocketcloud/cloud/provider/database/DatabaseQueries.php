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
 * @method static QueryBuilder addServerGroup(array $data)
 * @method static QueryBuilder removeServerGroup(string $name)
 * @method static QueryBuilder editServerGroup(string $name, array $newData)
 * @method static QueryBuilder getServerGroup(string $name)
 * @method static QueryBuilder checkServerGroup(string $name)
 * @method static QueryBuilder getServerGroups()
 * @method static QueryBuilder setModuleState(string $module, bool $enabled)
 * @method static QueryBuilder getModuleState(string $module)
 * @method static QueryBuilder enablePlayerNotifications(string $player)
 * @method static QueryBuilder disablePlayerNotifications(string $player)
 * @method static QueryBuilder hasNotificationsEnabled(string $player)
 * @method static QueryBuilder addToWhitelist(string $player)
 * @method static QueryBuilder removeFromWhitelist(string $player)
 * @method static QueryBuilder isOnWhitelist(string $player)
 * @method static QueryBuilder getWhitelist()
 */
final class DatabaseQueries {
    use ParameterEnumTrait;

    protected static function init(): void {
        self::register("createTables", function (): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->create([
                    "name" => "VARCHAR(50) PRIMARY KEY",
                    "lobby" => "BOOL",
                    "maintenance" => "BOOL",
                    "static" => "BOOL",
                    "maxPlayerCount" => "INTEGER",
                    "minServerCount" => "INTEGER",
                    "maxServerCount" => "INTEGER",
                    "startNewPercentage" => "FLOAT",
                    "autoStart" => "BOOL",
                    "templateType" => "VARCHAR(10)"
                ])
                ->changeTable(DatabaseTables::SERVER_GROUPS)
                    ->create([
                        "name" => "VARCHAR(50) PRIMARY KEY",
                        "templates" => "TEXT"
                    ])
                ->changeTable(DatabaseTables::MODULES)
                    ->create([
                        "module" => "VARCHAR(100) PRIMARY KEY",
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

        self::register("addTemplate", function (array $data): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->insert($data);
        });

        self::register("removeTemplate", function (string $name): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->delete(["name" => $name]);
        });

        self::register("editTemplate", function (string $name, array $newData): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->update($newData, ["name" => $name]);
        });

        self::register("getTemplate", function (string $name): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->get(TemplateHelper::KEYS, ["name" => $name]);
        });

        self::register("checkTemplate", function (string $name): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->has(["name" => $name]);
        });

        self::register("getTemplates", function (): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->select(TemplateHelper::KEYS, "*");
        });

        self::register("addServerGroup", function (array $data): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::SERVER_GROUPS)
                ->insert($data);
        });

        self::register("removeServerGroup", function (string $name): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::SERVER_GROUPS)
                ->delete(["name" => $name]);
        });

        self::register("editServerGroup", function (string $name, array $newData): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::SERVER_GROUPS)
                ->update($newData, ["name" => $name]);
        });

        self::register("getServerGroup", function (string $name): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::SERVER_GROUPS)
                ->get(["name", "templates"], ["name" => $name]);
        });

        self::register("checkServerGroup", function (string $name): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::SERVER_GROUPS)
                ->has(["name" => $name]);
        });

        self::register("getServerGroups", function (): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::TEMPLATES)
                ->select(TemplateHelper::KEYS, "*");
        });

        self::register("setModuleState", function (string $module, bool $enabled): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::MODULES)
                ->update(["enabled" => $enabled], ["module" => $module]);
        });

        self::register("getModuleState", function (string $module): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::MODULES)
                ->get(["enabled"], ["module" => $module]);
        });

        self::register("enablePlayerNotifications", function (string $player): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::NOTIFICATIONS)
                ->insert(["player" => $player]);
        });

        self::register("disablePlayerNotifications", function (string $player): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::NOTIFICATIONS)
                ->delete(["player" => $player]);
        });

        self::register("hasNotificationsEnabled", function (string $player): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::NOTIFICATIONS)
                ->has(["player" => $player]);
        });

        self::register("addToWhitelist", function (string $player): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::MAINTENANCE_LIST)
                ->insert(["player" => $player]);
        });

        self::register("removeFromWhitelist", function (string $player): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::MAINTENANCE_LIST)
                ->delete(["player" => $player]);
        });

        self::register("isOnWhitelist", function (string $player): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::MAINTENANCE_LIST)
                ->has(["player" => $player]);
        });

        self::register("getWhitelist", function (): QueryBuilder {
            return QueryBuilder::table(DatabaseTables::MAINTENANCE_LIST)
                ->select(["player"], "*");
        });
    }
}