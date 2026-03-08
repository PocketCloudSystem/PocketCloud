<?php

namespace pocketcloud\cloud\migration;

use Closure;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\migration\impl\CloudPluginsMigrator;
use pocketcloud\cloud\migration\impl\JsonGroupsToMySqlMigrator;
use pocketcloud\cloud\migration\impl\JsonMaintenanceListToMySqlMigrator;
use pocketcloud\cloud\migration\impl\JsonModulesToMySqlMigrator;
use pocketcloud\cloud\migration\impl\JsonNotificationsToMySqlMigrator;
use pocketcloud\cloud\migration\impl\JsonTemplatesToMySqlMigrator;
use pocketcloud\cloud\migration\impl\OldNotifyListToNotificationsMigrator;
use pocketcloud\cloud\migration\impl\ProxyPluginsMigrator;
use pocketcloud\cloud\migration\impl\ServerPluginsMigrator;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class MigratorManager {
    use SingletonTrait;

    /** @var array<IMigrator> */
    private array $migrators = [];

    public function __construct() {
        self::setInstance($this);
        $this->add(new CloudPluginsMigrator());
        $this->add(new ServerPluginsMigrator());
        $this->add(new ProxyPluginsMigrator());
        $this->add(new OldNotifyListToNotificationsMigrator());
        $this->add(new JsonTemplatesToMySqlMigrator());
        $this->add(new JsonGroupsToMySqlMigrator());
        $this->add(new JsonModulesToMySqlMigrator());
        $this->add(new JsonMaintenanceListToMySqlMigrator());
        $this->add(new JsonNotificationsToMySqlMigrator());
    }

    public function migrateAll(): int {
        $failedMigrations = 0;
        $availableMigrations = array_filter($this->migrators, fn(IMigrator $migrator) => $migrator->requiresMigration());
        foreach ($availableMigrations as $migrator) {
            if (!$migrator->runOnStartup() && Server::getInstance()->getTick() == 0) continue;
            if (!$this->migrate($migrator)) {
                $failedMigrations++;
            }
        }

        return $failedMigrations;
    }

    public function migrate(IMigrator $migrator): bool {
        if (!$migrator->backup()) {
            CloudLogger::get()->error("Failed to create a backup for the migration §b{}§r, §ccancelling §rthe §bmigration process §rfor it...", $migrator->id());
            return false;
        }

        if ($migrator->migrate()) {
            CloudLogger::get()->success("Successfully migrated the data from §b{}", $migrator->id());
            return true;
        } else {
            CloudLogger::get()->error("Migration §b{} §rfailed, §rrolling back...", $migrator->id());
            if ($migrator->rollback()) {
                CloudLogger::get()->success("Rolled back the data for §b{}", $migrator->id());
            } else {
                CloudLogger::get()->error("Failed to rollback data for §b{} §rfailed, §rmanual intervention is required on backup directory path: §b{}", $migrator->id(), $migrator->currentBackupPath());
            }
        }

        return false;
    }

    public function add(IMigrator $migrator): void {
        $this->migrators[$migrator->id()] = $migrator;
    }

    public function remove(IMigrator|string $migrator): void {
        $migrator = $migrator instanceof IMigrator ? $migrator->id() : $migrator;
        if (isset($this->migrators[$migrator])) unset($this->migrators[$migrator]);
    }

    public function checkForAnyMigration(): bool {
        return array_any($this->migrators, fn(IMigrator $migrator) => $migrator->requiresMigration());
    }

    public function get(string $id): ?IMigrator {
        return $this->migrators[$id] ?? null;
    }

    public function getAll(?Closure $filter = null): array {
        if ($filter !== null) return array_filter($this->migrators, $filter);
        return $this->migrators;
    }
}