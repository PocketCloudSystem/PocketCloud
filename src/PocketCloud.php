<?php

namespace pocketcloud\cloud;

use Phar;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\command\CommandManager;
use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\crash\CrashDump;
use pocketcloud\cloud\event\impl\cloud\CloudStartedEvent;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\plugin\CloudPluginManager;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\scheduler\AsyncPool;
use pocketcloud\cloud\server\binary\BinaryDownloader;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\server\config\ServerPropertiesGenerator;
use pocketcloud\cloud\server\prepare\ServerPreparator;
use pocketcloud\cloud\software\SoftwareManager;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\thread\ThreadManager;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\misc\Queue;
use pocketcloud\cloud\library\LibraryManager;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\util\loader\ClassLoader;
use pocketcloud\cloud\util\misc\LoadableList;
use pocketcloud\cloud\util\misc\TickableList;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\TerminalUtils;
use pocketcloud\cloud\util\VersionInfo;
use pocketmine\snooze\SleeperHandler;
use Throwable;
use const pocketcloud\BINARIES_PATH;
use const pocketcloud\CLOUD_PATH;
use const pocketcloud\CRASHES_PATH;
use const pocketcloud\GLOBAL_TEMPLATES_PATH;
use const pocketcloud\IN_GAME_PATH;
use const pocketcloud\IS_PHAR;
use const pocketcloud\LIBRARIES_PATH;
use const pocketcloud\LOG_PATH;
use const pocketcloud\PLUGINS_PATH;
use const pocketcloud\SERVER_CRASHES_PATH;
use const pocketcloud\SERVER_GROUPS_PATH;
use const pocketcloud\SOFTWARE_PATH;
use const pocketcloud\STORAGE_PATH;
use const pocketcloud\TEMP_PATH;
use const pocketcloud\TEMPLATES_PATH;

final class PocketCloud {

    private static ?self $instance = null;

    private bool $running = false;
    private int $tick = 0;
    private float $startTimestamp = 0;

    private Console $console;
    private CommandManager $commandManager;
    private LibraryManager $libraryManager;
    private SleeperHandler $sleeperHandler;
    private Queue $startNotificationQueue;
    private MainConfig $config;
    private SoftwareManager $softwareManager;
    private ThreadManager $threadManager;
    private Network $network;
    private AsyncPool $asyncPool;
    private ServerPreparator $serverPreparator;
    private TemplateManager $templateManager;
    private ServerGroupManager $serverGroupManager;
    private ServerPropertiesGenerator $serverPropertiesGenerator;
    private CloudServerManager $serverManager;
    private TrafficMonitorManager $trafficMonitorManager;
    private CloudPluginManager $pluginManager;

    public function __construct(
        private readonly ClassLoader $classLoader
    ) {
        self::$instance = $this;
    }

    public function start(): void {
        if ($this->running) return;
        $this->startTimestamp = microtime(true);
        $this->running = true;

        $this->console = new Console();
        $this->commandManager = new CommandManager();
        ($this->libraryManager = new LibraryManager())->load();
        $this->sleeperHandler = new SleeperHandler();
        $this->startNotificationQueue = Queue::fromType([]);
        $this->config = new MainConfig();
        ($this->softwareManager = new SoftwareManager())->load();
        $this->softwareManager->downloadAll();
        $this->threadManager = new ThreadManager();
        $this->network = new Network(Address::read($this->config->getNetwork()));
        $this->asyncPool = new AsyncPool();
        $this->serverPreparator = new ServerPreparator();
        $this->templateManager = new TemplateManager();
        $this->serverGroupManager = new ServerGroupManager();
        $this->serverPropertiesGenerator = new ServerPropertiesGenerator();
        $this->serverManager = new CloudServerManager();
        $this->trafficMonitorManager = new TrafficMonitorManager();
        $this->pluginManager = new CloudPluginManager();

        $this->console->register();

        CloudProvider::select();

        if (array_any($this->config->getAllBinaries(), fn(string $url, string $templateType) => BinaryDownloader::downloadBinary($url, $templateType) === true)) {
            $this->addStartNotification("§8====== §cATTENTION! §8======", CloudLogLevel::WARN())
                ->addStartNotification("§rNew binaries have been downloaded.", CloudLogLevel::WARN())
                ->addStartNotification("Please make sure that they are NOT corrupted due to issues with PharData.", CloudLogLevel::WARN())
                ->addStartNotification("If they are, please download them manually.", CloudLogLevel::WARN())
                ->addStartNotification("Thank you.", CloudLogLevel::WARN());
        }

        TickableList::add(
            $this->trafficMonitorManager, $this->serverManager
        );

        LoadableList::add(
            $this->commandManager,
            $this->templateManager, $this->serverGroupManager,
            $this->serverPropertiesGenerator, $this->serverPreparator,
            $this->pluginManager
        );

        TerminalUtils::clear();
        CloudLogger::get()->emptyLine()->setCustomFormat("§r{message}")
            ->info("  §bPocket§3Cloud §8- §rA cloud system for pocketmine servers with proxy support §8- §b{} §8- §rdeveloped by §b{}", VersionInfo::VERSION . (VersionInfo::BETA ? "§c@BETA" : ""), implode("§8, §b", VersionInfo::DEVELOPERS))
            ->info("  Join our discord for information: §bhttps://discord.gg/3HbPEpaE3T")
            ->emptyLine()->resetCustomFormat();

        CloudLogger::get()->info("The §bCloud §ris §astarting§r...");

        $this->network->init();
        LoadableList::loadAll();
        $this->pluginManager->enableAll();

        while (($entry = $this->startNotificationQueue->next()) !== null) {
            CloudLogger::get()->log($entry[0], $entry[1], ...$entry[2]);
        }

        CloudLogger::get()->success("§bCloud §rhas been §astarted§r. §8(§rTook §b" . number_format($time = (microtime(true) - $this->startTimestamp), 3) . "s§8)");
        new CloudStartedEvent($time)->call();

        CloudServerManager::getInstance()->start(TemplateManager::getInstance()->get("sigma"));

        $this->tick();
    }

    public function crash(): void {
        if (!$this->running) return;
        try {
            $filePath = CrashDump::fromLastestError()->create();
            CloudLogger::get()->error("§cAn error has occurred and caused the Cloud to crash entirely.");
            CloudLogger::get()->error("§cA crashdump has been created.");
            CloudLogger::get()->error("§c(§b{}§c)", $filePath);
        } catch (Throwable $e) {
            CloudLogger::get()->error("§cFailed to create crashdump§8: §e" . $e->getMessage());
        }

        $this->shutdown();
        echo "--- Uptime: " . round($this->getUptime(), 3) . "s - PocketCloud has crashed, waiting 60s before completely killing the process. ---" . PHP_EOL;
        sleep(60);
        @TerminalUtils::kill(getmypid());
        exit(1);
    }

    public function shutdown(): void {
        if (!$this->running) return;
        CloudLogger::get()->info("§cShutting down §bPocket§3Cloud§r...");
        $this->running = false;

        $this->serverManager->stopAll(true);
        $this->network->close();
        $this->console->remove();
    }

    public function tick(): void {
        $start = microtime(true);
        while ($this->running) {
            $this->tick++;
            TickableList::tickAll($this->tick);
            $this->console->readLine();
            $this->sleeperHandler->sleepUntil($start);
        }
    }

    public function addStartNotification(string $logMessage, ?CloudLogLevel $logLevel = null, mixed... $params): self {
        $this->startNotificationQueue->add([$logLevel ?? CloudLogLevel::INFO(), $logMessage, $params]);
        return $this;
    }

    public function isRunning(): bool {
        return $this->running;
    }

    public function getTick(): int {
        return $this->tick;
    }

    public function getStartTimestamp(): float {
        return $this->startTimestamp;
    }

    public function getUptime(): float {
        if ($this->startTimestamp <= 0) return 0;
        return microtime(true) - $this->startTimestamp;
    }

    public function getConsole(): Console {
        return $this->console;
    }

    public function getCommandManager(): CommandManager {
        return $this->commandManager;
    }

    public function getLibraryManager(): LibraryManager {
        return $this->libraryManager;
    }

    public function getSleeperHandler(): SleeperHandler {
        return $this->sleeperHandler;
    }

    public function getStartNotificationQueue(): Queue {
        return $this->startNotificationQueue;
    }

    public function getConfig(): MainConfig {
        return $this->config;
    }

    public function getSoftwareManager(): SoftwareManager {
        return $this->softwareManager;
    }

    public function getThreadManager(): ThreadManager {
        return $this->threadManager;
    }

    public function getNetwork(): Network {
        return $this->network;
    }

    public function getAsyncPool(): AsyncPool {
        return $this->asyncPool;
    }

    public function getServerPreparator(): ServerPreparator {
        return $this->serverPreparator;
    }

    public function getTemplateManager(): TemplateManager {
        return $this->templateManager;
    }

    public function getServerGroupManager(): ServerGroupManager {
        return $this->serverGroupManager;
    }

    public function getServerPropertiesGenerator(): ServerPropertiesGenerator {
        return $this->serverPropertiesGenerator;
    }

    public function getServerManager(): CloudServerManager {
        return $this->serverManager;
    }

    public function getTrafficMonitorManager(): TrafficMonitorManager {
        return $this->trafficMonitorManager;
    }

    public function getPluginManager(): CloudPluginManager {
        return $this->pluginManager;
    }

    public function getClassLoader(): ClassLoader {
        return $this->classLoader;
    }

    public static function getInstance(): ?self {
        return self::$instance;
    }
}

error_reporting(-1);

checkPlatform();

$autoloadPath = dirname(__FILE__, 2) . "/vendor/autoload.php";
require_once $autoloadPath;

define("pocketcloud\VENDOR_AUTOLOAD_PATH", $autoloadPath);
define("pocketcloud\IS_PHAR", Phar::running() !== "");
define("pocketcloud\SOURCE_PATH", __DIR__ . DIRECTORY_SEPARATOR);

define("pocketcloud\CLOUD_PATH", (IS_PHAR ?
    str_replace("phar://", "", dirname(__DIR__, 2) . DIRECTORY_SEPARATOR) :
    dirname(__DIR__) . DIRECTORY_SEPARATOR
));

define("pocketcloud\STORAGE_PATH", CLOUD_PATH . "storage" . DIRECTORY_SEPARATOR);
define("pocketcloud\CRASHES_PATH", CLOUD_PATH . "storage" . DIRECTORY_SEPARATOR . "crashes" . DIRECTORY_SEPARATOR);
define("pocketcloud\SERVER_CRASHES_PATH", CRASHES_PATH . "servers" . DIRECTORY_SEPARATOR);
define("pocketcloud\BINARIES_PATH", STORAGE_PATH . "binaries" . DIRECTORY_SEPARATOR);
define("pocketcloud\LIBRARIES_PATH", STORAGE_PATH . "libraries" . DIRECTORY_SEPARATOR);
define("pocketcloud\PLUGINS_PATH", STORAGE_PATH . "plugins" . DIRECTORY_SEPARATOR);
define("pocketcloud\SOFTWARE_PATH", STORAGE_PATH . "software" . DIRECTORY_SEPARATOR);
define("pocketcloud\IN_GAME_PATH", STORAGE_PATH . "inGame" . DIRECTORY_SEPARATOR);
define("pocketcloud\STATIC_SERVERS_PATH", STORAGE_PATH . "staticServers" . DIRECTORY_SEPARATOR);
define("pocketcloud\LOG_PATH", STORAGE_PATH . "cloud.log");
define("pocketcloud\TEMP_PATH", CLOUD_PATH . "tmp" . DIRECTORY_SEPARATOR);
define("pocketcloud\TEMPLATES_PATH", CLOUD_PATH . "templates" . DIRECTORY_SEPARATOR);
define("pocketcloud\GLOBAL_TEMPLATES_PATH", TEMPLATES_PATH . "global" . DIRECTORY_SEPARATOR);
define("pocketcloud\SERVER_GROUPS_PATH", CLOUD_PATH . "groups" . DIRECTORY_SEPARATOR);
define("pocketcloud\FIRST_RUN", !file_exists(STORAGE_PATH . "config.json"));

foreach ([
    STORAGE_PATH, CRASHES_PATH, SERVER_CRASHES_PATH, BINARIES_PATH, LIBRARIES_PATH, PLUGINS_PATH, SOFTWARE_PATH, IN_GAME_PATH, LOG_PATH,
    TEMP_PATH,
    TEMPLATES_PATH, GLOBAL_TEMPLATES_PATH,
    SERVER_GROUPS_PATH
] as $dir) {
    FileUtils::createDir($dir);
}

if (checkRunning($pid)) {
    die("[ERROR] PocketCloud is already running in a different process. (PID: $pid)" . PHP_EOL);
}

$classLoader = new ClassLoader();
$classLoader->init();

$lockFile = createLockFile();

do {
    $cloud = new PocketCloud($classLoader);
    $cloud->start();

    if (ThreadManager::getInstance()->stopAll() > 0) {
        CloudLogger::get()->warn("Some threads crashed while trying to stop them, force-kill of the process...");
        @TerminalUtils::kill(getmypid());
    }
} while (false);

releaseLockFile($lockFile);

exit(0);

function checkRunning(?int &$pid = null): bool {
    if (!file_exists(STORAGE_PATH)) return false;
    $file = fopen(STORAGE_PATH . "cloud.lock", "a+b");
    if ($file === false) return false;
    if (!flock($file, LOCK_EX | LOCK_NB)) {
        flock($file, LOCK_SH);
        $processId = stream_get_contents($file);
        if (preg_match('/^\d+$/', $processId) === 1) $pid = $processId;
        return true;
    }

    return false;
}

function createLockFile(): mixed {
    $file = fopen(STORAGE_PATH . "cloud.lock", "a+b");
    if ($file === false) return null;
    if (!flock($file, LOCK_EX | LOCK_NB)) flock($file, LOCK_SH);
    ftruncate($file, 0);
    fwrite($file, (string) getmypid());
    fflush($file);
    flock($file, LOCK_SH);
    return $file;
}

function releaseLockFile(mixed $lockFile): void {
    flock($lockFile, LOCK_UN);
    fclose($lockFile);
    unlink(STORAGE_PATH . "cloud.lock");
}

function checkPlatform(): void {
    if (PHP_OS_FAMILY == "Windows") {
        die("[ERROR] You can't use PocketCloud on a windows machine.");
    }

    $messages = [];

    if (version_compare("8.4.0", PHP_VERSION) > 0) {
        $messages[] = "PHP >=8.4.0 is required, but you have PHP " . PHP_VERSION . ".";
    }

    foreach ([
        "curl" => "cURL",
        "date" => "Date",
        "igbinary" => "igbinary",
        "json" => "JSON",
        "mbstring" => "Multibyte String",
        "pcre" => "PCRE",
        "phar" => "Phar",
        "pmmpthread" => "pmmpthread",
        "reflection" => "Reflection",
        "sockets" => "Sockets",
        "spl" => "SPL",
        "yaml" => "YAML",
        "zip" => "Zip",
        "zlib" => "Zlib",
        "pcntl" => "pcntl"
    ] as $extension => $displayName) {
        if (!extension_loaded($extension)) {
            $messages[] = "[ERROR] Missing $displayName ($extension) as a extension.";
        }
    }

    if (($pmmpThreadVersion = phpversion("pmmpthread")) !== false){
        if (version_compare($pmmpThreadVersion, "6.1.0") < 0 || version_compare($pmmpThreadVersion, "7.0.0") >= 0) {
            $messages[] = "[WARN] pmmpthread ^6.1.0 is required, while you have $pmmpThreadVersion.";
        }
    }

    if (count($messages) > 0) {
        foreach ($messages as $message) {
            echo $message . PHP_EOL;
        }
        exit;
    }
}