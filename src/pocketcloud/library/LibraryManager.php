<?php

namespace pocketcloud\library;

use pocketcloud\console\log\Logger;
use pocketcloud\PocketCloud;
use pocketcloud\util\SingletonTrait;
use Throwable;

class LibraryManager {
    use SingletonTrait;

    /** @var array<Library> */
    private array $libraries = [];

    public function __construct() {
        self::setInstance($this);
        $this->addLibrary(new Library(
            "Snooze",
            "https://github.com/pmmp/Snooze/archive/refs/heads/master.zip",
            LIBRARY_PATH . "snooze.zip",
            LIBRARY_PATH . "snooze/",
            "pocketmine\\snooze\\",
            LIBRARY_PATH . "snooze/pocketmine/snooze/",
            ["composer.json", "README.md"],
            LIBRARY_PATH . "snooze/Snooze-master/src/",
            LIBRARY_PATH . "snooze/pocketmine/snooze/",
            LIBRARY_PATH . "snooze/Snooze-master/"
        ));

        $this->addLibrary(new Library(
            "configlib",
            "https://github.com/r3pt1s/configlib/archive/refs/heads/main.zip",
            LIBRARY_PATH . "configlib.zip",
            LIBRARY_PATH . "config/",
            "configlib\\",
            LIBRARY_PATH . "config/configlib/",
            ["README.md"],
            LIBRARY_PATH . "config/configlib-main/src/",
            LIBRARY_PATH . "config/",
            LIBRARY_PATH . "config/configlib-main/",
        ));

        $this->addLibrary(new Library(
            "pmforms",
            "https://github.com/dktapps-pm-pl/pmforms/archive/refs/heads/master.zip",
            LIBRARY_PATH . "pmforms.zip",
            LIBRARY_PATH . "pmforms/",
            null,
            null,
            ["README.md", "virion.yml", ".github/"],
            LIBRARY_PATH . "pmforms/pmforms-master/src/",
            LIBRARY_PATH . "pmforms/",
            LIBRARY_PATH . "pmforms/pmforms-master/",
            true
        ));
    }

    public function load(): void {
        foreach ($this->libraries as $library) {
            if (!$library->exists()) {
                $temporaryLogger = new Logger(saveMode: false);
                try {
                    $temporaryLogger->info("Start downloading library: %s (%s)", $library->getName(), $library->getDownloadUrl());
                    if ($library->download()) {
                        $temporaryLogger->info("Successfully downloaded library: %s (%s)", $library->getName(), $library->getUnzipLocation());
                    } else {
                        $temporaryLogger->warn("Failed to downloaded library: %s", $library->getName());
                    }
                } catch (Throwable $exception) {
                    $temporaryLogger->warn("Failed to downloaded library: %s", $library->getName());
                    $temporaryLogger->exception($exception);
                }
            }

            if ($library->canBeLoaded()) PocketCloud::getInstance()->getClassLoader()->addPath($library->getClassLoadFolder(), $library->getClassLoadPath());
        }
    }

    public function addLibrary(Library $library): void {
        $this->libraries[$library->getName()] = $library;
    }

    public function removeLibrary(Library $library): void {
        if (isset($this->libraries[$library->getName()])) unset($this->libraries[$library->getName()]);
    }

    public function getLibrary(string $name): ?Library {
        return $this->libraries[$name] ?? null;
    }

    public function getLibraries(): array {
        return $this->libraries;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}