<?php

namespace pocketcloud\cloud\update\def;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\update\IUpdateChecker;
use pocketcloud\cloud\util\promise\Promise;
use RuntimeException;

final class CloudPluginsUpdateChecker implements IUpdateChecker {

    public function needsUpdate(): Promise {
        $promise = new Promise();

        $i = 0;
        $result = [];
        $amount = count($all = TemplateType::getAll());
        foreach ($all as $type) {
            $type->checkForUpdate()->then(function (bool $needsUpdate) use ($promise, &$i, &$result, $type, $amount): void {
                $i++;
                if ($needsUpdate) {
                    CloudLogger::get()->info("Your version of plugin §b{} §ris outdated...", $type->getRelativeBridgeFileLocation());
                    $result[] = $type->getName();
                }

                if ($i == $amount) $promise->resolve([count($result) > 0, $result]);
            })->failure(function () use(&$i, $amount, $promise, &$result, $type): void {
                CloudLogger::get()->warn("Failed to check updates for plugin §b{}§r.", $type->getRelativeBridgeFileLocation());
                $i++;
                if ($i == $amount) $promise->resolve([count($result) > 0, $result]);
            });
        }

        return $promise;
    }

    public function update(mixed $extraData): Promise {
        $promise = new Promise();
        $i = 0;
        $pluginList = $extraData;
        if (!is_array($pluginList)) throw new RuntimeException("This shouldn't happen");
        foreach ($pluginList as $type) {
            $type = TemplateType::get($type);
            if ($type instanceof TemplateType) {
                $type->update()->then(function (bool $success) use($type, $promise, &$i, $pluginList): void {
                    $i++;
                    if (!$success) {
                        CloudLogger::get()->warn("Failed to update plugin §b{}§r.", $type->getRelativeBridgeFileLocation());
                        $promise->reject();
                        return;
                    }

                    CloudLogger::get()->info("Successfully §adownloaded §rthe latest version of §b{}§r.", $type->getRelativeBridgeFileLocation());

                    if ($i == count($pluginList)) {
                        $promise->resolve();
                    }
                })->reject(function () use($promise, $type): void {
                    CloudLogger::get()->warn("Failed to update plugin §b{}§r, some error happened.", $type->getName());
                    $promise->reject();
                });
            } else throw new RuntimeException("TemplateType should not be null on update");
        }

        return $promise;
    }
}