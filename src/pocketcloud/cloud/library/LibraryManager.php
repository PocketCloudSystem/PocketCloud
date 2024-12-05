<?php

namespace pocketcloud\cloud\library;

use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\SingletonTrait;
use Throwable;

final class LibraryManager {
    use SingletonTrait;

    /** @var array<Library> */
    private array $libraries = [];

    public function __construct() {
        self::setInstance($this);
        $this->add(new Library(
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

        $this->add(new Library(
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

        $this->add(new Library(
            "mysql",
            "https://github.com/PocketCloudSystem/mysqllib/archive/refs/heads/main.zip",
            LIBRARY_PATH . "mysqllib.zip",
            LIBRARY_PATH . "mysql/",
            "r3pt1s\\mysql\\",
            LIBRARY_PATH . "mysql/r3pt1s/mysql/",
            ["README.md"],
            LIBRARY_PATH . "mysql/mysqllib-main/src/",
            LIBRARY_PATH . "mysql/",
            LIBRARY_PATH . "mysql/mysqllib-main/",
        ));

        $this->add(new Library(
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
                $temporaryLogger = CloudLogger::temp(false);
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

    public function add(Library $library): void {
        $this->libraries[$library->getName()] = $library;
    }

    public function remove(Library $library): void {
        if (isset($this->libraries[$library->getName()])) unset($this->libraries[$library->getName()]);
    }

    public function get(string $name): ?Library {
        return $this->libraries[$name] ?? null;
    }

    public function getAll(): array {
        return $this->libraries;
    }
}