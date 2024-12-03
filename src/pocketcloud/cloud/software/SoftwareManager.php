<?php

namespace pocketcloud\cloud\software;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\net\NetUtils;
use pocketcloud\cloud\util\SingletonTrait;

final class SoftwareManager {
    use SingletonTrait;

    /** @var array<Software> */
    private array $software = [];

    public function __construct() {
        self::setInstance($this);
        $this->registerSoftware(new Software("PocketMine-MP", MainConfig::getInstance()->getStartCommand("server"), "https://github.com/pmmp/PocketMine-MP/releases/latest/download/PocketMine-MP.phar", "PocketMine-MP.phar", ["pmmp"]));
        $this->registerSoftware(new Software("WaterdogPE", MainConfig::getInstance()->getStartCommand("proxy"), "https://github.com/WaterdogPE/WaterdogPE/releases/download/latest/Waterdog.jar", "Waterdog.jar", ["wdpe"]));
    }

    public function downloadAll(): void {
        foreach ($this->software as $software) {
            if (!$this->isDownloaded($software)) {
                $this->downloadSoftware($software);
            }
        }
    }

    public function downloadSoftware(Software $software): void {
        $temporaryLogger = CloudLogger::temp(true, false);
        $temporaryLogger->info("Start downloading software: %s (%s)", $software->getName(), $software->getUrl());
        $result = NetUtils::download($software->getUrl(), SOFTWARE_PATH . $software->getFileName());
        if (!$result) {
            $temporaryLogger->warn("Failed to downloaded software: %s", $software->getName());
            return;
        }

        $temporaryLogger->info("Successfully downloaded software: %s (%s)", $software->getName(), SOFTWARE_PATH . $software->getFileName());
    }

    public function isDownloaded(Software $software): bool {
        return file_exists(SOFTWARE_PATH . $software->getFileName());
    }

    public function registerSoftware(Software $software): bool {
        if (!isset($this->software[$software->getName()])) {
            $this->software[$software->getName()] = $software;
            return true;
        }
        return false;
    }

    public function unregisterSoftware(Software $software): bool {
        if (isset($this->software[$software->getName()])) {
            unset($this->software[$software->getName()]);
            return true;
        }
        return false;
    }

    public function getSoftwareByName(string $name): ?Software {
        foreach ($this->software as $software) {
            if ($software->getName() == $name || in_array($name, $software->getAliases())) return $software;
        }
        return null;
    }

    public function getSoftware(): array {
        return $this->software;
    }
}