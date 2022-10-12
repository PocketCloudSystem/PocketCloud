<?php

namespace pocketcloud\software;

use pocketcloud\event\impl\software\SoftwareDownloadEvent;
use pocketcloud\event\impl\software\SoftwareRegisterEvent;
use pocketcloud\event\impl\software\SoftwareUnregisterEvent;
use pocketcloud\scheduler\AsyncClosureTask;
use pocketcloud\scheduler\AsyncPool;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\SingletonTrait;
use pocketcloud\utils\Utils;

class SoftwareManager {
    use SingletonTrait;

    /** @var array<Software> */
    private array $software;

    public function __construct() {
        self::setInstance($this);
        $this->registerSoftware(new Software("PocketMine-MP", Utils::getBinary() . " " . SOFTWARE_PATH . "PocketMine-MP.phar --no-wizard", "https://github.com/pmmp/PocketMine-MP/releases/latest/download/PocketMine-MP.phar", "PocketMine-MP.phar", ["pmmp"]));
        $this->registerSoftware(new Software("WaterdogPE", "java -jar " . SOFTWARE_PATH . "Waterdog.jar", "https://jenkins.waterdog.dev/job/Waterdog/job/WaterdogPE/job/master/lastSuccessfulBuild/artifact/target/Waterdog.jar", "Waterdog.jar", ["wdpe"]));
    }

    public function downloadAll() {
        foreach ($this->software as $software) {
            if (!$this->isDownloaded($software)) {
                $this->downloadSoftware($software);
            }
        }
    }

    public function downloadSoftware(Software $software) {
        (new SoftwareDownloadEvent($software))->call();
        CloudLogger::get()->info("Downloading §e" . $software->getName() . " §rsoftware...");

        $url = $software->getUrl();
        $fileName = $software->getFileName();
        AsyncPool::getInstance()->submitTask(AsyncClosureTask::fromClosure(
            fn() => Utils::download($url, SOFTWARE_PATH . $fileName),
            function(bool $result) use($software) {
                if (!$result) {
                    CloudLogger::get()->error("§cCan't download the §e" . $software->getName() . " §csoftware!");
                    return;
                }

                CloudLogger::get()->info("Successfully downloaded §e" . $software->getName() . " §rsoftware!");
            }
        ));
    }

    public function isDownloaded(Software $software): bool {
        return file_exists(SOFTWARE_PATH . $software->getFileName());
    }

    public function registerSoftware(Software $software): bool {
        if (!isset($this->software[$software->getName()])) {
            (new SoftwareRegisterEvent($software))->call();
            $this->software[$software->getName()] = $software;
            return true;
        }
        return false;
    }

    public function unregisterSoftware(Software $software): bool {
        if (isset($this->software[$software->getName()])) {
            (new SoftwareUnregisterEvent($software))->call();
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