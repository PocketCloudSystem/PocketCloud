<?php

namespace pocketcloud\cloud\provider;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\module\InGameModule;
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
            DatabaseQueries::getModuleState($value)
                ->execute(fn (bool $v) => InGameModule::setModuleState($value, $v));
        }
    }

    public function addTemplate(Template $template): void {
        DatabaseQueries::addTemplate($template->toArray())->execute();
    }

    public function removeTemplate(Template $template): void {
        DatabaseQueries::removeTemplate($template->getName())->execute();
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
            ->execute(fn(bool $result) => $promise->resolve($result));

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

    public function setModuleState(string $module, bool $enabled): void {
        InGameModule::setModuleState($module, $enabled);
        DatabaseQueries::setModuleState($module, $enabled)->execute();
    }

    public function getModuleState(string $module): Promise {
        $promise = new Promise();

        DatabaseQueries::getModuleState($module)
            ->execute(fn(bool $enabled) => $promise->resolve($enabled));

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
            ->execute(fn(bool $enabled) => $promise->resolve($enabled));

        return $promise;
    }

    public function addToWhitelist(string $player): void {
        DatabaseQueries::addToWhitelist($player)->execute();
    }

    public function removeFromWhitelist(string $player): void {
        DatabaseQueries::removeFromWhitelist($player)->execute();
    }

    public function isOnWhitelist(string $player): Promise {
        $promise = new Promise();

        DatabaseQueries::isOnWhitelist($player)
            ->execute(fn(bool $enabled) => $promise->resolve($enabled));

        return $promise;
    }

    public function getConnectionPool(): ?ConnectionPool {
        return $this->connectionPool;
    }
}