<?php

namespace pocketcloud\cloud;

use Phar;
use pocketcloud\cloud\exception\ExceptionHandler;
use pocketcloud\cloud\library\LibraryManager;
use pocketcloud\cloud\loader\ClassLoader;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\terminal\log\handler\ShutdownHandler;
use pocketcloud\cloud\terminal\log\logger\LoggingCache;
use pocketcloud\cloud\terminal\Terminal;
use pocketcloud\cloud\thread\ThreadManager;
use pocketcloud\cloud\util\terminal\TerminalUtils;
use pocketcloud\cloud\util\Utils;
use pocketmine\snooze\SleeperHandler;

final class PocketCloud {

    private static ?self $instance = null;

    private bool $running = true;
    private int $tick = 0;

    private SleeperHandler $sleeperHandler;
    private Terminal $terminal;

    public function __construct(
        private readonly ClassLoader $classLoader
    ) {
        self::$instance = $this;

        $this->startUp();

        $this->sleeperHandler = new SleeperHandler();
        $this->terminal = new Terminal();

        $this->terminal->start();
        $this->tick();
    }

    public function startUp(): void {
        if (Utils::checkRunning($pid)) {
            CloudLogger::get()->error("Another instance of §bPocket§3Cloud §ris already running! (ProcessId: " . $pid . ")");
            exit(1);
        }

        Utils::createLockFile();

        ExceptionHandler::set();
        ShutdownHandler::register();

        LibraryManager::getInstance()->load();
        TerminalUtils::clear();
    }

    public function tick(): void {
        $start = microtime(true);
        while ($this->running) {
            $this->sleeperHandler->sleepUntil($start);
            usleep(50 * 1000);
            $this->tick++;
        }
    }

    public function shutdown(): void {
        ShutdownHandler::unregister();
        ThreadManager::getInstance()->stopAll();
        Utils::deleteLockFile();
        CloudLogger::close();
        LoggingCache::clear();
        TerminalUtils::kill(getmypid());
    }

    public function getTerminal(): Terminal {
        return $this->terminal;
    }

    public function getSleeperHandler(): SleeperHandler {
        return $this->sleeperHandler;
    }

    public function getClassLoader(): ClassLoader {
        return $this->classLoader;
    }

    public static function getInstance(): ?self {
        return self::$instance;
    }
}

require_once "loader/ClassLoader.php";
require_once "PocketCloud.php";

define("IS_PHAR", Phar::running() !== "");
define("SOURCE_PATH", __DIR__ . "/");

if (IS_PHAR) {
    define("CLOUD_PATH", str_replace("phar://", "", dirname(__DIR__, 4) . DIRECTORY_SEPARATOR));
} else {
    define("CLOUD_PATH", dirname(__DIR__, 3) . DIRECTORY_SEPARATOR);
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
define("WEB_PATH", STORAGE_PATH . "web/");
define("LOG_PATH", STORAGE_PATH . "cloud.log");
define("TEMP_PATH", CLOUD_PATH . "tmp/");
define("TEMPLATES_PATH", CLOUD_PATH . "templates/");
define("FIRST_RUN", !file_exists(STORAGE_PATH . "config.json"));

if (!file_exists(STORAGE_PATH)) mkdir(STORAGE_PATH);
if (!file_exists(LIBRARY_PATH)) mkdir(LIBRARY_PATH);
if (!file_exists(CRASH_PATH)) mkdir(CRASH_PATH);
if (!file_exists(PLUGINS_PATH)) mkdir(PLUGINS_PATH);
if (!file_exists(SERVER_PLUGINS_PATH)) mkdir(SERVER_PLUGINS_PATH);
if (!file_exists(PROXY_PLUGINS_PATH)) mkdir(PROXY_PLUGINS_PATH);
if (!file_exists(CLOUD_PLUGINS_PATH)) mkdir(CLOUD_PLUGINS_PATH);
if (!file_exists(SOFTWARE_PATH)) mkdir(SOFTWARE_PATH);
if (!file_exists(IN_GAME_PATH)) mkdir(IN_GAME_PATH);
if (!file_exists(WEB_PATH)) mkdir(WEB_PATH);
if (!file_exists(LOG_PATH)) file_put_contents(LOG_PATH, "");
if (!file_exists(TEMPLATES_PATH)) mkdir(TEMPLATES_PATH);
if (!file_exists(TEMP_PATH)) mkdir(TEMP_PATH);

$classLoader = new ClassLoader();
$classLoader->init();

new PocketCloud($classLoader);