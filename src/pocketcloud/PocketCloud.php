<?php

namespace pocketcloud;

use pocketcloud\command\CommandManager;
use pocketcloud\config\DefaultConfig;
use pocketcloud\config\MaintenanceList;
use pocketcloud\config\ModuleConfig;
use pocketcloud\config\NotifyList;
use pocketcloud\console\Console;
use pocketcloud\console\log\CloudLogSaver;
use pocketcloud\event\EventManager;
use pocketcloud\event\impl\cloud\CloudStartedEvent;
use pocketcloud\http\HttpServer;
use pocketcloud\language\Language;
use pocketcloud\library\LibraryManager;
use pocketcloud\network\Network;
use pocketcloud\plugin\CloudPluginManager;
use pocketcloud\scheduler\AsyncPool;
use pocketcloud\server\CloudServerManager;
use pocketcloud\setup\impl\ConfigSetup;
use pocketcloud\setup\impl\TemplateSetup;
use pocketcloud\setup\Setup;
use pocketcloud\software\SoftwareManager;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateManager;
use pocketcloud\template\TemplateType;
use pocketcloud\thread\ThreadManager;
use pocketcloud\update\UpdateChecker;
use pocketcloud\util\Address;
use pocketcloud\util\AsyncExecutor;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\ExceptionHandler;
use pocketcloud\util\ReloadableList;
use pocketcloud\util\ShutdownHandler;
use pocketcloud\util\TickableList;
use pocketcloud\util\Utils;
use pocketcloud\util\VersionInfo;
use pocketmine\snooze\SleeperHandler;

class PocketCloud {

    private static self $instance;
    private bool $running = true;
    private bool $reloading = false;
    private int $tick = 0;
    private SleeperHandler $sleeperHandler;
    private Console $console;
    private Network $network;
    private HttpServer $httpServer;

    public function __construct() {
        self::$instance = $this;

        ExceptionHandler::set();
        ShutdownHandler::register();

        Utils::createDefaultFiles();
        LibraryManager::getInstance()->load();

        Utils::check();
        $this->sleeperHandler = new SleeperHandler();
        $this->console = new Console();
        $this->console->start();

        Utils::downloadFiles();
        SoftwareManager::getInstance()->downloadAll();
        Utils::clearConsole();
        if (FIRST_RUN) {
            (new ConfigSetup())->completion(function(array $results): void {
                $this->start();
                if ($results["defaultLobbyTemplate"] ?? true) {
                    TemplateManager::getInstance()->createTemplate(new Template(
                        "Lobby", true, true, false, 20, 1, 3, true, false, TemplateType::SERVER()
                    ));
                }

                if ($results["defaultProxyTemplate"] ?? true) {
                    TemplateManager::getInstance()->createTemplate(new Template(
                        "Proxy", false, true, false, 20, 1, 1, false, false, TemplateType::PROXY()
                    ));
                }
            })->startSetup();
        } else $this->start();
        $this->tick();
    }

    public function start() {
        ini_set("memory_limit", ($memory = DefaultConfig::getInstance()->getMemoryLimit()) > 0 ? $memory . "M" : "-1");

        CloudLogger::get()->info("§bPocket§3Cloud §8(§ev" . VersionInfo::VERSION . (VersionInfo::BETA ? "@BETA" : "") . "§8) - §rdeveloped by §e" . implode("§8, §e", VersionInfo::DEVELOPERS));
        if (Language::current()->getName() == "English") CloudLogger::get()->info("You can join our discord for information: §ehttps://discord.gg/3HbPEpaE3T");
        else CloudLogger::get()->info("Du kannst unserem Discord für Informationen beitreten: §ehttps://discord.gg/3HbPEpaE3T");
        CloudLogger::get()->emptyLine();

        CloudLogger::get()->info(Language::current()->translate("cloud.starting"));
        $startTime = microtime(true);
        Utils::createLockFile();

        $this->network = new Network(new Address("127.0.0.1", DefaultConfig::getInstance()->getNetworkPort()));
        $this->httpServer = new HttpServer(new Address("0.0.0.0", DefaultConfig::getInstance()->getHttpServerPort()));

        TemplateManager::getInstance()->loadTemplates();
        CloudPluginManager::getInstance()->loadPlugins();
        CloudPluginManager::getInstance()->enablePlugins();

        TickableList::add(CloudPluginManager::getInstance());
        TickableList::add(AsyncPool::getInstance());
        TickableList::add(CloudServerManager::getInstance());
        TickableList::add(TemplateManager::getInstance());
        ReloadableList::add(CommandManager::getInstance());
        ReloadableList::add(CloudPluginManager::getInstance());
        ReloadableList::add(TemplateManager::getInstance());
        ReloadableList::add(new MaintenanceList());
        ReloadableList::add(new NotifyList());
        ReloadableList::add(DefaultConfig::getInstance());
        ReloadableList::add(ModuleConfig::getInstance());
        ReloadableList::add($this->httpServer);

        $this->network->init();

        if (DefaultConfig::getInstance()->isHttpServerEnabled()) {
            $this->httpServer->init();
        }

        UpdateChecker::getInstance()->check();

        $startedTime = (microtime(true) - $startTime);
        (new CloudStartedEvent($startedTime))->call();
        CloudLogger::get()->info(Language::current()->translate("cloud.started", number_format($startedTime, 3)));
        if (count(TemplateManager::getInstance()->getTemplates()) == 0 && FIRST_RUN) {
            CloudLogger::get()->info(Language::current()->translate("cloud.no.templates"));
            (new TemplateSetup())->startSetup();
        }

        $this->network->start();
    }

    public function reload() {
        if (!$this->reloading) {
            $this->reloading = true;
            $startTime = microtime(true);
            CloudLogger::get()->info(Language::current()->translate("reload.start"));
            $currentLanguage = Language::current()->getName();
            ReloadableList::reload();
            $startedTime = (microtime(true) - $startTime);
            $newLanguage = Language::current()->getName();
            if ($currentLanguage !== $newLanguage) {
                DefaultConfig::getInstance()->setLanguage($currentLanguage);
                CloudLogger::get()->warn(Language::current()->translate("reload.change.language", $newLanguage));
            }

            CloudLogger::get()->info(Language::current()->translate("reload.success", number_format($startedTime, 3)));

            $this->reloading = false;
        }
    }

    public function tick() {
        $start = microtime(true);
        while ($this->running) {
            $this->sleeperHandler->sleepUntil($start);
            usleep(50 * 1000);
            $this->tick++;
            TickableList::tick($this->tick);
        }
    }

    public function shutdown() {
        if ($this->running) {
            $this->running = false;
            Setup::getCurrentSetup()?->cancel();
            ShutdownHandler::unregister();
            EventManager::getInstance()->removeAll();
            CloudServerManager::getInstance()->stopAll(true);
            CloudPluginManager::getInstance()->disablePlugins();
            AsyncPool::getInstance()->shutdown();
            if (isset($this->network)) $this->network->close();
            if (isset($this->network)) $this->console->quit();
            if (isset($this->network)) $this->httpServer->close();
            ThreadManager::getInstance()->stopAll();
            Utils::deleteLockFile();
            CloudLogger::get()->info(Language::current()->translate("cloud.stopped"));
            CloudLogger::close();
            CloudLogSaver::clear();
            Utils::kill(getmypid(), true);
        }
    }

    public function getHttpServer(): HttpServer {
        return $this->httpServer;
    }

    public function getNetwork(): Network {
        return $this->network;
    }

    public function getConsole(): Console {
        return $this->console;
    }

    public function getSleeperHandler(): SleeperHandler {
        return $this->sleeperHandler;
    }

    public function getTick(): int {
        return $this->tick;
    }

    public function isReloading(): bool {
        return $this->reloading;
    }

    public function isRunning(): bool {
        return $this->running;
    }

    public static function getInstance(): PocketCloud {
        return self::$instance;
    }
}

require_once "PocketCloud.php";

define("SOURCE_PATH", __DIR__ . "/");

if (\Phar::running()) {
    define("CLOUD_PATH", str_replace("phar://", "", dirname(__DIR__, 3) . DIRECTORY_SEPARATOR));
} else {
    define("CLOUD_PATH", dirname(__DIR__, 2) . DIRECTORY_SEPARATOR);
}

define("STORAGE_PATH", CLOUD_PATH . "storage/");
define("CRASH_PATH", CLOUD_PATH . "storage/crashes/");
define("LIBRARY_PATH", STORAGE_PATH . "libraries/");
define("PLUGINS_PATH", STORAGE_PATH . "plugins/");
define("SERVER_PLUGINS_PATH", STORAGE_PATH . "plugins/server/");
define("PROXY_PLUGINS_PATH", STORAGE_PATH . "plugins/proxy/");
define("CLOUD_PLUGINS_PATH", STORAGE_PATH . "plugins/cloud/");
define("SOFTWARE_PATH", STORAGE_PATH . "software/");
define("IN_GAME_PATH", STORAGE_PATH . "inGame/");
define("LOG_PATH", STORAGE_PATH . "cloud.log");
define("TEMP_PATH", CLOUD_PATH . "tmp/");
define("TEMPLATES_PATH", CLOUD_PATH . "templates/");
define("FIRST_RUN", !file_exists(STORAGE_PATH . "config.json"));

spl_autoload_register(function($class) {
    if (str_starts_with($class, "pocketcloud\\")) $file = __DIR__ . DIRECTORY_SEPARATOR . str_replace(["\\", "\\\\", "/", "//"], DIRECTORY_SEPARATOR, str_replace("pocketcloud\\", "", $class)) . ".php";
    else $file = __DIR__ . str_replace(["\\", "\\\\", "/", "//"], DIRECTORY_SEPARATOR, $class) . ".php";
    if (!class_exists($class) and file_exists($file)) require_once $file;
});

new PocketCloud();