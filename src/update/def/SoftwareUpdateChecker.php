<?php

namespace pocketcloud\cloud\update\def;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\software\ServerSoftware;
use pocketcloud\cloud\software\ServerSoftwareManager;
use pocketcloud\cloud\update\IUpdateChecker;
use pocketcloud\cloud\util\promise\Promise;
use RuntimeException;

final class SoftwareUpdateChecker implements IUpdateChecker {

    public function needsUpdate(): Promise {
        $promise = new Promise();

        $i = 0;
        $result = [];
        $amount = count($all = ServerSoftwareManager::getInstance()->getAll());
        foreach ($all as $software) {
            $software->checkForUpdate()->then(function (bool $needsUpdate) use ($promise, &$i, &$result, $software, $amount): void {
                $i++;
                if ($needsUpdate) {
                    if (!MainConfig::getInstance()->isExecuteUpdates()) PocketCloud::getInstance()->addStartNotification("Your version of §b{} §ris outdated. Please update it manually.", CloudLogLevel::WARN(), $software->getName());
                    CloudLogger::get()->info("Your version of §b{} §ris outdated...", $software->getName());
                    $result[] = $software->getName();
                }

                if ($i == $amount) $promise->resolve([count($result) > 0, $result]);
            })->failure(function () use(&$i, $amount, $promise, &$result, $software): void {
                CloudLogger::get()->warn("Failed to check updates for §b{}§r.", $software->getName());
                $i++;
                if ($i == $amount) $promise->resolve([count($result) > 0, $result]);
            });
        }

        return $promise;
    }

    public function update(mixed $extraData): Promise {
        $promise = new Promise();
        $i = 0;
        $softwareList = $extraData;
        if (!is_array($softwareList)) throw new RuntimeException("This shouldn't happen");
        foreach ($softwareList as $software) {
            $software = ServerSoftwareManager::getInstance()->get($software);
            if ($software instanceof ServerSoftware) {
                $software->update()->then(function (bool $success) use($software, $promise, &$i, $softwareList): void {
                    $i++;
                    if (!$success) {
                        CloudLogger::get()->warn("Failed to update software §b{}§r.", $software->getName());
                        $promise->reject();
                        return;
                    }

                    CloudLogger::get()->info("Successfully §adownloaded §rthe latest version of §b{}§r.", $software->getName());

                    if ($i == count($softwareList)) {
                        $promise->resolve();
                    }
                })->reject(function () use($promise, $software): void {
                    CloudLogger::get()->warn("Failed to update software §b{}§r, some error happened.", $software->getName());
                    $promise->reject();
                });
            } else throw new RuntimeException("Software should not be null on update");
        }

        return $promise;
    }
}