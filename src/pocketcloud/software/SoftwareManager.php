<?php

namespace pocketcloud\software;

use JetBrains\PhpStorm\Pure;
use pocketcloud\config\impl\DefaultConfig;
use pocketcloud\console\log\Logger;
use pocketcloud\util\SingletonTrait;
use pocketcloud\util\Utils;

class SoftwareManager {
    use SingletonTrait;

    /** @var array<Software> */
    private array $software = [];

    public function __construct() {
        self::setInstance($this);
        $this->registerSoftware(new Software("PocketMine-MP", DefaultConfig::getInstance()->getStartCommand("server"), "https://github.com/pmmp/PocketMine-MP/releases/latest/download/PocketMine-MP.phar", "PocketMine-MP.phar", ["pmmp"]));
        $this->registerSoftware(new Software("WaterdogPE", DefaultConfig::getInstance()->getStartCommand("proxy"), "https://github.com/WaterdogPE/WaterdogPE/releases/download/latest/Waterdog.jar", "Waterdog.jar", ["wdpe"]));
    }

    public function downloadAll(): void {
        foreach ($this->software as $software) {
            if (!$this->isDownloaded($software)) {
                $this->downloadSoftware($software);
            }
        }
    }

    public function downloadSoftware(Software $software): void {
        $temporaryLogger = new Logger(saveMode: false);
        $temporaryLogger->info("Start downloading software: %s (%s)", $software->getName(), $software->getUrl());
        $result = Utils::download($software->getUrl(), SOFTWARE_PATH . $software->getFileName());
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

    #[Pure] public function getSoftwareByName(string $name): ?Software {
        foreach ($this->software as $software) {
            if ($software->getName() == $name || in_array($name, $software->getAliases())) return $software;
        }
        return null;
    }

    public function getSoftware(): array {
        return $this->software;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}