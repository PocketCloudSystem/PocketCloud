<?php

namespace pocketcloud\cloud\provider;

use pocketcloud\cloud\cache\MaintenanceList;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\cache\InGameModule;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\provider\database\DatabaseQueries;
use pocketcloud\cloud\provider\migration\MigrationList;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\promise\Promise;
use r3pt1s\mysql\ConnectionPool;
use Throwable;

final class CloudMySqlProvider extends CloudProvider {

    private ?ConnectionPool $connectionPool;

    public function __construct() {
        $this->connectionPool = new ConnectionPool([
            "address" => MainConfig::getInstance()->getMySqlAddress(),
            "user" => MainConfig::getInstance()->getMySqlUser(),
            "password" => MainConfig::getInstance()->getMySqlPassword(),
            "database" => MainConfig::getInstance()->getMySqlDatabase(),
            "port" => MainConfig::getInstance()->getMySqlPort()
        ], 1, PocketCloud::getInstance()->getSleeperHandler(), function (Throwable $throwable): void {
            CloudLogger::get()->error("Something unexpected happened while executing a mysql query...");
            CloudLogger::get()->exception($throwable);
        });

        DatabaseQueries::createTables()->execute(function (): void {
            if (MigrationList::JSON_TO_MYSQL()->checkForMigration()) {
                MigrationList::JSON_TO_MYSQL()->migrate();
            }
        });

        foreach (InGameModule::getAll() as $value) {
            $this->getModuleState($value)->then(fn(bool $v) => InGameModule::setModuleState($value, $v));
        }

        $this->getWhitelist()->then(fn(array $list) => MaintenanceList::sync($list));
    }

    public function addTemplate(Template $template): void {
        DatabaseQueries::addTemplate($template->toArray())->execute();
    }

    public function removeTemplate(Template $template): void {
        DatabaseQueries::removeTemplate($template->getName())->execute();
    }

    public function editTemplate(Template $template, array $newData): void {
        DatabaseQueries::editTemplate($template->getName(), $newData)->execute();
    }

    public function getTemplate(string $template): Promise {
        $promise = new Promise();

        DatabaseQueries::getTemplate($template)
            ->execute(function (?array $result) use($promise): void {
                if (!is_array($result)) {
                    $promise->reject();
                    return;
                }

                if (($template = Template::fromArray($result)) !== null) {
                    $promise->resolve($template);
                } else $promise->reject();
            });

        return $promise;
    }

    public function checkTemplate(string $template): Promise {
        $promise = new Promise();

        DatabaseQueries::checkTemplate($template)
            ->execute(fn(?bool $check) => $promise->resolve($check ?? false));

        return $promise;
    }

    public function getTemplates(): Promise {
        $promise = new Promise();

        DatabaseQueries::getTemplates()
            ->execute(function (?array $result) use($promise): void {
                if (!is_array($result)) {
                    $promise->reject();
                    return;
                }

                $templates = [];
                foreach ($result as $data) {
                    if (($template = Template::fromArray($data)) !== null) {
                        $templates[$template->getName()] = $template;
                    }
                }

                $promise->resolve($templates);
            });

        return $promise;
    }

    public function addServerGroup(ServerGroup $serverGroup): void {
        DatabaseQueries::addServerGroup($serverGroup->toArray(true))->execute();
    }

    public function removeServerGroup(ServerGroup $serverGroup): void {
        DatabaseQueries::removeServerGroup($serverGroup->getName())->execute();
    }

    public function editServerGroup(ServerGroup $serverGroup, array $newData): void {
        if (is_array($newData["templates"])) $newData["templates"] = json_encode($newData["templates"]);
        DatabaseQueries::editServerGroup($serverGroup->getName(), $newData)->execute();
    }

    public function getServerGroup(string $serverGroup): Promise {
        $promise = new Promise();

        DatabaseQueries::getServerGroup($serverGroup)
            ->execute(function (?array $result) use($promise): void {
                if (!is_array($result)) {
                    $promise->reject();
                    return;
                }

                if (($serverGroup = ServerGroup::fromArray($result)) !== null) {
                    $promise->resolve($serverGroup);
                } else $promise->reject();
            });

        return $promise;
    }

    public function checkServerGroup(string $serverGroup): Promise {
        $promise = new Promise();

        DatabaseQueries::checkServerGroup($serverGroup)
            ->execute(fn(?bool $check) => $promise->resolve($check ?? false));

        return $promise;
    }

    public function getServerGroups(): Promise {
        $promise = new Promise();

        DatabaseQueries::getServerGroups()
            ->execute(function (?array $result) use($promise): void {
                if (!is_array($result)) {
                    $promise->reject();
                    return;
                }

                $serverGroups = [];
                foreach ($result as $data) {
                    if (($serverGroup = ServerGroup::fromArray($data)) !== null) {
                        $serverGroups[$serverGroup->getName()] = $serverGroup;
                    }
                }

                $promise->resolve($serverGroups);
            });

        return $promise;
    }

    public function setModuleState(string $module, bool $enabled): void {
        InGameModule::setModuleState($module, $enabled);
        DatabaseQueries::setModuleState($module, $enabled)->execute();
    }

    public function getModuleState(string $module): Promise {
        $promise = new Promise();

        DatabaseQueries::getModuleState($module)
            ->execute(fn(array $result) => $promise->resolve($result["enabled"] == 1));

        return $promise;
    }

    public function enablePlayerNotifications(string $player): void {
        DatabaseQueries::enablePlayerNotifications($player)->execute();
    }

    public function disablePlayerNotifications(string $player): void {
        DatabaseQueries::disablePlayerNotifications($player)->execute();
    }

    public function hasNotificationsEnabled(string $player): Promise {
        $promise = new Promise();

        DatabaseQueries::hasNotificationsEnabled($player)
            ->execute(fn(?bool $enabled) => $promise->resolve($enabled ?? false));

        return $promise;
    }

    public function addToWhitelist(string $player): void {
        DatabaseQueries::addToWhitelist($player)->execute();
        MaintenanceList::add($player);
    }

    public function removeFromWhitelist(string $player): void {
        DatabaseQueries::removeFromWhitelist($player)->execute();
        MaintenanceList::remove($player);
    }

    public function isOnWhitelist(string $player): Promise {
        $promise = new Promise();

        DatabaseQueries::isOnWhitelist($player)
            ->execute(fn(?bool $enabled) => $promise->resolve($enabled ?? false));

        return $promise;
    }

    public function getWhitelist(): Promise {
        $promise = new Promise();

        DatabaseQueries::getWhitelist()
            ->execute(fn(?array $list) => $promise->resolve($list === null ? [] : array_map(fn(array $r) => $r["player"], $list)));

        return $promise;
    }

    public function getConnectionPool(): ?ConnectionPool {
        return $this->connectionPool;
    }
}