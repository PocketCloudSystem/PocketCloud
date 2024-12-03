<?php

namespace pocketcloud\cloud;

use Phar;
use pocketcloud\cloud\exception\ExceptionHandler;
use pocketcloud\cloud\library\LibraryManager;
use pocketcloud\cloud\loader\ClassLoader;
use pocketcloud\cloud\server\util\ServerUtils;
use pocketcloud\cloud\software\SoftwareManager;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\terminal\log\handler\ShutdownHandler;
use pocketcloud\cloud\terminal\log\logger\LoggingCache;
use pocketcloud\cloud\terminal\Terminal;
use pocketcloud\cloud\thread\ThreadManager;
use pocketcloud\cloud\util\terminal\TerminalUtils;
use pocketcloud\cloud\util\tick\TickableList;
use pocketcloud\cloud\util\Utils;
use pocketcloud\cloud\util\VersionInfo;
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

        if (PHP_OS_FAMILY == "Windows") {
            CloudLogger::get()->error("You can't use §bPocket§3Cloud §ron Windows!");
            exit(1);
        }

        if (!ServerUtils::checkBinary()) {
            CloudLogger::get()->error("Please install the following php binary in " . CLOUD_PATH . ":");
            CloudLogger::get()->error("§ehttps://jenkins.pmmp.io/job/PHP-8.0-Aggregate/lastSuccessfulBuild/artifact/PHP-8.0-Linux-x86_64.tar.gz");
            exit(1);
        }

        if (!ServerUtils::checkJava()) {
            CloudLogger::get()->error("Please install Java 17!");
            CloudLogger::get()->error("Your operating system: §e" . php_uname());
            exit(1);
        }

        if (!ServerUtils::detectStartMethod()) {
            CloudLogger::get()->error("Please install one of the following software:");
            CloudLogger::get()->error("tmux (apt-get install tmux)");
            CloudLogger::get()->error("Screen (apt-get install screen)");
            exit(1);
        }

        Utils::createLockFile();

        ExceptionHandler::set();
        ShutdownHandler::register();

        LibraryManager::getInstance()->load();
        SoftwareManager::getInstance()->downloadAll();
        TerminalUtils::clear();

        CloudLogger::get()->info("§bPocket§3Cloud §8(§ev" . VersionInfo::VERSION . (VersionInfo::BETA ? "§c@BETA" : "") . "§8) - §rdeveloped by §e" . implode("§8, §e", VersionInfo::DEVELOPERS));
        CloudLogger::get()->info("You can join our discord for information: §ehttps://discord.gg/3HbPEpaE3T");
        CloudLogger::get()->emptyLine();
    }

    public function tick(): void {
        $start = microtime(true);
        while ($this->running) {
            $this->sleeperHandler->sleepUntil($start);
            usleep(50 * 1000);
            $this->tick++;
            TickableList::tick($this->tick);
        }
    }

    public function shutdown(): void {
        if (!$this->running) return;
        $this->running = false;
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