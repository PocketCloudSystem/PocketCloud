<?php

namespace pocketcloud;

use ErrorException;
use pocketcloud\command\CommandManager;
use pocketcloud\config\CloudConfig;
use pocketcloud\config\MaintenanceConfig;
use pocketcloud\config\MessagesConfig;
use pocketcloud\config\ModulesConfig;
use pocketcloud\config\NotifyConfig;
use pocketcloud\config\SignLayoutConfig;
use pocketcloud\console\Console;
use pocketcloud\event\EventManager;
use pocketcloud\event\impl\cloud\CloudStartedEvent;
use pocketcloud\exception\ExceptionHandler;
use pocketcloud\lib\snooze\SleeperHandler;
use pocketcloud\network\Network;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\plugin\PluginManager;
use pocketcloud\rest\RestAPI;
use pocketcloud\scheduler\AsyncPool;
use pocketcloud\scheduler\TaskScheduler;
use pocketcloud\server\CloudServerManager;
use pocketcloud\software\SoftwareManager;
use pocketcloud\task\CheckServerAmountTask;
use pocketcloud\task\ServerTimeoutTask;
use pocketcloud\template\TemplateManager;
use pocketcloud\update\UpdateChecker;
use pocketcloud\utils\Address;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\ShutdownHandler;
use pocketcloud\utils\Utils;
use pocketcloud\utils\VersionInfo;

class PocketCloud {

    private static self $instance;
    private CloudConfig $config;
    private MessagesConfig $messagesConfig;
    private NotifyConfig $notifyConfig;
    private ModulesConfig $modulesConfig;
    private MaintenanceConfig $maintenanceConfig;
    private SignLayoutConfig $signLayoutConfig;
    private SleeperHandler $sleeperHandler;
    private TaskScheduler $taskScheduler;
    private AsyncPool $asyncPool;
    private EventManager $eventManager;
    private CommandManager $commandManager;
    private Console $console;
    private PluginManager $pluginManager;
    private SoftwareManager $softwareManager;
    private TemplateManager $templateManager;
    private CloudPlayerManager $cloudPlayerManager;
    private CloudServerManager $cloudServerManager;
    private RestAPI $restAPI;
    private UpdateChecker $updateChecker;
    private Network $network;
    private bool $reloading = false;
    private bool $running = false;

    /** @throws ErrorException */
    public function __construct() {
        self::$instance = $this;

        Utils::createDefaultFiles();

        $this->config = new CloudConfig();
        $this->messagesConfig = new MessagesConfig();
        $this->notifyConfig = new NotifyConfig();
        $this->modulesConfig = new ModulesConfig();
        $this->maintenanceConfig = new MaintenanceConfig();
        $this->signLayoutConfig = new SignLayoutConfig();

        Utils::check();
        Utils::createLockFile();

        $this->sleeperHandler = new SleeperHandler();

        $this->running = true;

        $startTime = microtime(true);

        CloudLogger::get()->emptyLine();
        CloudLogger::get()->info("§bPocket§3Cloud §8(§e" . VersionInfo::VERSION . "§8) - §fdeveloped by §e" . implode("§8, §e", VersionInfo::DEVELOPERS));
        CloudLogger::get()->emptyLine();

        CloudLogger::get()->info("Starting cloud...");

        $this->taskScheduler = new TaskScheduler();
        $this->asyncPool = new AsyncPool();
        ExceptionHandler::set();
        ShutdownHandler::register();
        $this->eventManager = new EventManager();
        $this->commandManager = new CommandManager();
        $this->console = new Console();
        $this->pluginManager = new PluginManager();
        $this->softwareManager = new SoftwareManager();
        $this->templateManager = new TemplateManager();
        $this->cloudPlayerManager = new CloudPlayerManager();
        $this->cloudServerManager = new CloudServerManager();
        $this->restAPI = new RestAPI();
        $this->updateChecker = new UpdateChecker();
        $this->network = new Network(new Address("127.0.0.1", $this->config->getCloudPort()));

        CloudLogger::get()->info("Loading all plugins...");
        $this->pluginManager->loadPlugins();
        CloudLogger::get()->info("Enabling all plugins...");
        $this->pluginManager->enablePlugins();

        CloudLogger::get()->info("Loading all templates...");
        $this->templateManager->loadTemplates();

        $this->console->start();
        $this->restAPI->init();

        $startedTime = (microtime(true) - $startTime);
        (new CloudStartedEvent($startedTime))->call();
        CloudLogger::get()->info("Cloud was §astarted §rin §e" . number_format($startedTime, 3) . "s§r.");

        $this->softwareManager->downloadAll();

        Utils::downloadFiles();

        $this->taskScheduler->scheduleRepeatingTask(new ServerTimeoutTask(), 20);
        $this->taskScheduler->scheduleRepeatingTask(new CheckServerAmountTask(), 20);

        $this->tick();
    }

    public function reload() {
        $startTime = microtime(true);
        CloudLogger::get()->info("Reloading...");
        $this->reloading = true;

        $this->config->reload();
        $this->messagesConfig->reload();
        $this->notifyConfig->reload();
        $this->modulesConfig->reload();
        $this->maintenanceConfig->reload();
        $this->signLayoutConfig->reload();

        $this->pluginManager->reload();

        $this->reloading = false;
        CloudLogger::get()->info("Cloud was §areloaded §rin §e" . number_format((microtime(true) - $startTime), 3) . "s§r.");
        $this->updateChecker->check();
    }

    private function tick(): void {
        $start = microtime(true);
        while (true) {
            $this->sleeperHandler->sleepUntil($start);
            usleep(50 * 1000);
            $this->taskScheduler->onUpdate();
            $this->asyncPool->onUpdate();
        }
    }

    public function shutdown() {
        if ($this->running) {
            $this->running = false;
            ShutdownHandler::unregister();
            CloudLogger::get()->debug("Force stopping all servers");
            $this->cloudServerManager->stopAll(true);
            CloudLogger::get()->debug("Disabling all plugins");
            $this->pluginManager->disablePlugins();
            CloudLogger::get()->debug("Cancelling all tasks");
            $this->taskScheduler->cancelAll();
            CloudLogger::get()->debug("Closing network socket");
            $this->network->close();
            CloudLogger::get()->debug("Stopping console thread");
            CloudLogger::get()->debug("Unregistering the shutdown handler");
            CloudLogger::get()->info("Cloud was §cstopped§r.");
            CloudLogger::get()->close();
            Utils::deleteLockFile();
            Utils::kill(getmypid());
        }
    }

    public function getConfig(): CloudConfig {
        return $this->config;
    }

    public function getMessagesConfig(): MessagesConfig {
        return $this->messagesConfig;
    }

    public function getNotifyConfig(): NotifyConfig {
        return $this->notifyConfig;
    }

    public function getModulesConfig(): ModulesConfig {
        return $this->modulesConfig;
    }

    public function getMaintenanceConfig(): MaintenanceConfig {
        return $this->maintenanceConfig;
    }

    public function getSignLayoutConfig(): SignLayoutConfig {
        return $this->signLayoutConfig;
    }

    public function getSleeperHandler(): SleeperHandler {
        return $this->sleeperHandler;
    }

    public function getTaskScheduler(): TaskScheduler {
        return $this->taskScheduler;
    }

    public function getAsyncPool(): AsyncPool {
        return $this->asyncPool;
    }

    public function getEventManager(): EventManager {
        return $this->eventManager;
    }

    public function getCommandManager(): CommandManager {
        return $this->commandManager;
    }

    public function getConsole(): Console {
        return $this->console;
    }

    public function getPluginManager(): PluginManager {
        return $this->pluginManager;
    }

    public function getSoftwareManager(): SoftwareManager {
        return $this->softwareManager;
    }

    public function getTemplateManager(): TemplateManager {
        return $this->templateManager;
    }

    public function getCloudPlayerManager(): CloudPlayerManager {
        return $this->cloudPlayerManager;
    }

    public function getCloudServerManager(): CloudServerManager {
        return $this->cloudServerManager;
    }

    public function getRestAPI(): RestAPI {
        return $this->restAPI;
    }

    public function getUpdateChecker(): UpdateChecker {
        return $this->updateChecker;
    }

    public function getNetwork(): Network {
        return $this->network;
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

if (\Phar::running()) {
    define("CLOUD_PATH", str_replace("phar://", "", dirname(__DIR__, 3) . DIRECTORY_SEPARATOR));
} else {
    define("CLOUD_PATH", dirname(__DIR__, 2) . DIRECTORY_SEPARATOR);
}

define("SOURCE_PATH", __DIR__ . "/");
define("STORAGE_PATH", CLOUD_PATH . "storage/");
define("CRASH_PATH", CLOUD_PATH . "storage/crashes/");
define("PLUGINS_PATH", CLOUD_PATH . "storage/plugins/");
define("SERVER_PLUGINS_PATH", CLOUD_PATH . "storage/plugins/server/");
define("PROXY_PLUGINS_PATH", CLOUD_PATH . "storage/plugins/proxy/");
define("CLOUD_PLUGINS_PATH", CLOUD_PATH . "storage/plugins/cloud/");
define("SOFTWARE_PATH", CLOUD_PATH . "storage/software/");
define("IN_GAME_PATH", CLOUD_PATH . "storage/inGame/");
define("LOG_PATH", CLOUD_PATH . "storage/cloud.log");
define("TEMP_PATH", CLOUD_PATH . "tmp/");
define("TEMPLATES_PATH", CLOUD_PATH . "templates/");

spl_autoload_register(function($class) {
    if (str_starts_with($class, "pocketcloud\\")) $file = __DIR__ . DIRECTORY_SEPARATOR . str_replace(["\\", "\\\\", "/", "//"], DIRECTORY_SEPARATOR, str_replace("pocketcloud\\", "", $class)) . ".php";
    else $file = __DIR__ . str_replace(["\\", "\\\\", "/", "//"], DIRECTORY_SEPARATOR, $class) . ".php";
    if (!class_exists($class) and file_exists($file)) require_once $file;
});

new PocketCloud();