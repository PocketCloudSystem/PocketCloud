<?php

namespace pocketcloud\cloud\provider;

use Exception;
use pocketcloud\cloud\cache\InGameModuleCache;
use pocketcloud\cloud\cache\MaintenanceListCache;
use pocketcloud\cloud\cache\NotificationListCache;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\migration\IMigrator;
use pocketcloud\cloud\migration\MigratorManager;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\provider\database\DatabaseQueries;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\PathUtils;
use r3pt1s\mysql\ConnectionPool;
use pocketcloud\cloud\util\promise\Promise;
use r3pt1s\mysql\query\MySQLQuery;

final class CloudMySqlProvider extends CloudProvider {

    private ?ConnectionPool $connectionPool;

    public function __construct() {
        $this->connectionPool = new ConnectionPool(MainConfig::getInstance()->getMysqlSettings(), 1, PocketCloud::getInstance()->getSleeperHandler(), function (MySQLQuery|null $query, Exception $exception): void {
            CloudLogger::get()->error("Something unexpected happened while executing a mysql query... §8(§b{}§8)", $query === null ? "Unknown" : PathUtils::clean($query::class));
            CloudLogger::get()->exception($exception);
        });

        DatabaseQueries::createTables()->execute()->then(function (): void {
            foreach (MigratorManager::getInstance()->getAll(fn(IMigrator $migrator) => str_starts_with($migrator->id(), "migrate-json-")) as $migrator) {
                if ($migrator->requiresMigration()) {
                    MigratorManager::getInstance()->migrate($migrator);
                }
            }
        });

        foreach (InGameModuleCache::getAll() as $value) {
            $this->getModuleState($value)->then(function (?bool $v) use($value): void {
                if ($v === null) DatabaseQueries::insertModuleState($value, $v = false);
                InGameModuleCache::setModuleState($value, $v);
            });
        }

        $this->getWhitelist()->then(fn(array $list) => MaintenanceListCache::sync($list));
        $this->getNotificationList()->then(fn(array $list) => NotificationListCache::sync($list));
    }

    public function addTemplate(Template $template): Promise {
        $promise = new Promise();

        DatabaseQueries::addTemplate($template->write())->execute()
            ->then(fn() => $promise->resolve())
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function removeTemplate(Template $template): Promise {
        $promise = new Promise();

        DatabaseQueries::removeTemplate($template->getName())->execute()
            ->then(fn() => $promise->resolve())
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function editTemplate(Template $template, array $newData): Promise {
        $promise = new Promise();

        DatabaseQueries::editTemplate($template->getName(), $newData)->execute()
            ->then(fn() => $promise->resolve())
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function getTemplate(string $template): Promise {
        $promise = new Promise();

        DatabaseQueries::getTemplate($template)
            ->execute()->then(function (?array $result) use($promise): void {
                if (!is_array($result)) {
                    $promise->reject();
                    return;
                }

                if (($template = Template::read($result)) !== null) {
                    $promise->resolve($template);
                } else $promise->reject();
            })->failure(fn() => $promise->reject());

        return $promise;
    }

    public function checkTemplate(string $template): Promise {
        $promise = new Promise();

        DatabaseQueries::checkTemplate($template)
            ->execute()->then(fn(?bool $check) => $promise->resolve($check ?? false))
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function getTemplates(): Promise {
        $promise = new Promise();

        DatabaseQueries::getTemplates()
            ->execute()->then(function (?array $result) use($promise): void {
                if (!is_array($result)) {
                    $promise->reject();
                    return;
                }

                $templates = [];
                foreach ($result as $data) {
                    if (($template = Template::read($data)) !== null) {
                        $templates[$template->getName()] = $template;
                    }
                }

                $promise->resolve($templates);
            })
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function addServerGroup(ServerGroup $serverGroup): Promise {
        $promise = new Promise();

        $data = $serverGroup->write();
        if (is_array($data["templates"])) $data["templates"] = json_encode($data["templates"]);
        DatabaseQueries::addServerGroup($data)->execute()
            ->then(fn() => $promise->resolve())
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function removeServerGroup(ServerGroup $serverGroup): Promise {
        $promise = new Promise();

        DatabaseQueries::removeServerGroup($serverGroup->getName())->execute();

        return $promise;
    }

    public function editServerGroup(ServerGroup $serverGroup, array $newData): Promise {
        $promise = new Promise();

        if (is_array($newData["templates"])) $newData["templates"] = json_encode($newData["templates"]);
        DatabaseQueries::editServerGroup($serverGroup->getName(), $newData)->execute()
            ->then(fn() => $promise->resolve($newData)
            ->failure(fn() => $promise->reject()));

        return $promise;
    }

    public function getServerGroup(string $serverGroup): Promise {
        $promise = new Promise();

        DatabaseQueries::getServerGroup($serverGroup)
            ->execute()->then(function (?array $result) use($promise): void {
                if (!is_array($result)) {
                    $promise->reject();
                    return;
                }

                if (($serverGroup = ServerGroup::read($result)) !== null) {
                    $promise->resolve($serverGroup);
                } else $promise->reject();
            })
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function checkServerGroup(string $serverGroup): Promise {
        $promise = new Promise();

        DatabaseQueries::checkServerGroup($serverGroup)
            ->execute()->then(fn(?bool $check) => $promise->resolve($check ?? false))
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function getServerGroups(): Promise {
        $promise = new Promise();

        DatabaseQueries::getServerGroups()
            ->execute()->then(function (?array $result) use($promise): void {
                if (!is_array($result)) {
                    $promise->reject();
                    return;
                }

                $serverGroups = [];
                foreach ($result as $data) {
                    if (($serverGroup = ServerGroup::read($data)) !== null) {
                        $serverGroups[$serverGroup->getName()] = $serverGroup;
                    }
                }

                $promise->resolve($serverGroups);
            })
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function setModuleState(string $module, bool $enabled): Promise {
        $promise = new Promise();

        InGameModuleCache::setModuleState($module, $enabled);
        DatabaseQueries::checkModuleState($module)->execute()
            ->then(fn(bool $has) => $has ?
                DatabaseQueries::setModuleState($module, $enabled)->execute()
                    ->then(fn() => $promise->resolve())
                    ->failure(fn() => $promise->reject()) :
                DatabaseQueries::insertModuleState($module, $enabled)->execute()
                    ->then(fn() => $promise->resolve())
                    ->failure(fn() => $promise->reject())
            );

        return $promise;
    }

    public function getModuleState(string $module): Promise {
        $promise = new Promise();

        DatabaseQueries::getModuleState($module)
            ->execute()->then(fn(?array $result) => $promise->resolve(is_array($result) ? $result["enabled"] == 1 : null))
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function enablePlayerNotifications(string $player): Promise {
        $promise = new Promise();

        DatabaseQueries::enablePlayerNotifications($player)->execute()
            ->then(fn() => $promise->resolve())
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function disablePlayerNotifications(string $player): Promise {
        $promise = new Promise();

        DatabaseQueries::disablePlayerNotifications($player)->execute()
            ->then(fn() => $promise->resolve())
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function hasNotificationsEnabled(string $player): Promise {
        $promise = new Promise();

        DatabaseQueries::hasNotificationsEnabled($player)
            ->execute()->then(fn(?bool $enabled) => $promise->resolve($enabled ?? false));

        return $promise;
    }

    public function getNotificationList(): Promise {
        $promise = new Promise();

        DatabaseQueries::getNotificationList()
            ->execute()->then(fn(?array $list) => $promise->resolve($list === null ? [] : array_map(fn(array $r) => $r["player"], $list)));

        return $promise;
    }

    public function addToWhitelist(string $player): Promise {
        $promise = new Promise();

        MaintenanceListCache::add($player);
        DatabaseQueries::addToWhitelist($player)->execute()
            ->then(fn() => $promise->resolve())
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function removeFromWhitelist(string $player): Promise {
        $promise = new Promise();

        MaintenanceListCache::remove($player);
        DatabaseQueries::removeFromWhitelist($player)->execute()
            ->then(fn() => $promise->resolve())
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function isOnWhitelist(string $player): Promise {
        $promise = new Promise();

        DatabaseQueries::isOnWhitelist($player)
            ->execute()->then(fn(?bool $enabled) => $promise->resolve($enabled ?? false))
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function getWhitelist(): Promise {
        $promise = new Promise();

        DatabaseQueries::getWhitelist()
            ->execute()->then(fn(?array $list) => $promise->resolve($list === null ? [] : array_map(fn(array $r) => $r["player"], $list)))
            ->failure(fn() => $promise->reject());

        return $promise;
    }

    public function getConnectionPool(): ?ConnectionPool {
        return $this->connectionPool;
    }
}