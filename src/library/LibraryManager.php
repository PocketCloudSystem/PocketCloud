<?php

namespace pocketcloud\cloud\library;

use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\config\type\ConfigTypeList;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\update\UpdateChecker;
use pocketcloud\cloud\util\misc\Loadable;
use pocketcloud\cloud\util\trait\SingletonTrait;
use RuntimeException;
use const pocketcloud\LIBRARIES_PATH;

final class LibraryManager implements Loadable {
    use SingletonTrait;

    /** @var array<Library> */
    private array $libraries = [];
    private Config $libraryConfig;

    public function __construct() {
        self::setInstance($this);
    }

    public function load(): void {
        $this->libraryConfig = new Config(LIBRARIES_PATH . "libraries.json", ConfigTypeList::JSON());

        foreach ([
            new Library(
                "config",
                "https://github.com/r3pt1s/configlib/archive/refs/heads/main.zip",
                "",
                "src/",
                false
            ),
            new Library(
                "snooze",
                "https://github.com/pmmp/Snooze/archive/refs/tags/0.5.0.zip",
                "pocketmine\\snooze",
                "src/",
                false
            ),
            new Library(
                "mysql",
                "https://github.com/PocketCloudSystem/mysqllib/archive/refs/heads/main.zip",
                "",
                "src/",
                false
            ),
            new Library(
                "BetterForms",
                "https://github.com/PocketCloudSystem/BetterForms/archive/refs/heads/main.zip",
                "",
                "src/",
                true
            ),
            new Library(
                "bStats-PMMP",
                "https://github.com/PocketCloudSystem/bStats-PMMP/archive/refs/heads/master.zip",
                "",
                "src/",
                false
            ),
            new Library(
                "discord-webhook-lib",
                "https://github.com/PocketCloudSystem/discord-webhook-lib/archive/refs/heads/main.zip",
                "",
                "src/",
                false
            )
        ] as $library) {
            $this->libraryConfig->set($library->getName(), $library->write());
        }

        $this->libraryConfig->save();

        foreach ($this->libraryConfig->getAll() as $library) {
            if (($library = Library::read($library)) !== null) {
                CloudLogger::get()->info("Loading library: {}", $library->getName());
                $this->libraries[$library->getName()] = $library;
                if (!$library->check()) {
                    if (!$library->download())
                        throw new RuntimeException("Library '" . $library->getName() . "' is not available");
                }

                if (!$library->load() && !$library->isBridgeOnly())
                    throw new RuntimeException("Library '" . $library->getName() . "' with namespace folder: " . $library->getNamespaceFolder() . " is not available");
            }
        }
    }

    public function checkForUpdates(bool $force = false): int {
        if (!MainConfig::getInstance()->canCheckForUpdates(UpdateChecker::TYPE_LIBRARIES) && !$force) {
            PocketCloud::getInstance()->addStartNotification("Skipped updates for §b{}§8, §ras it is disabled in the config.", CloudLogLevel::DEBUG(), UpdateChecker::TYPE_LIBRARIES);
            return -1;
        }

        $updatedLibs = 0;
        foreach ($this->libraries as $library) {
            if ($library->needsAnUpdate()) {
                $updatedLibs++;
                $library->download(true, $force);
            }
        }

        return $updatedLibs;
    }

    public function add(Library $library): bool {
        if ($this->libraryConfig->has($library->getName())) return false;
        $this->libraries[$library->getName()] = $library;
        $this->libraryConfig->set($library->getName(), $library->write());
        $this->libraryConfig->save();
        return true;
    }

    public function get(string $name): ?Library {
        return $this->libraries[$name] ?? null;
    }

    public function getAll(): array {
        return $this->libraries;
    }
}