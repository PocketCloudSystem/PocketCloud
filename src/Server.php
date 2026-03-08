<?php

namespace pocketcloud\cloud;

use pocketcloud\cloud\config\impl\LogSettingsConfig;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\config\impl\ServerSettingsConfig;
use pocketcloud\cloud\console\command\CommandManager;
use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\console\log\logger\MainLogger;
use pocketcloud\cloud\console\log\output\OutputManager;
use pocketcloud\cloud\console\screen\ScreenManager;
use pocketcloud\cloud\crash\CrashDump;
use pocketcloud\cloud\event\impl\cloud\CloudStartedEvent;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\http\HttpServer;
use pocketcloud\cloud\http\HttpServerBuilder;
use pocketcloud\cloud\http\route\impl\HealthRoute;
use pocketcloud\cloud\http\route\impl\v1\group\AddTemplatesToGroupRoute;
use pocketcloud\cloud\http\route\impl\v1\group\CreateGroupRoute;
use pocketcloud\cloud\http\route\impl\v1\group\GroupInfoRoute;
use pocketcloud\cloud\http\route\impl\v1\group\ListGroupsRoute;
use pocketcloud\cloud\http\route\impl\v1\group\RemoveGroupRoute;
use pocketcloud\cloud\http\route\impl\v1\group\RemoveTemplatesFromGroupRoute;
use pocketcloud\cloud\http\route\impl\v1\maintenance\ListMaintenanceRoute;
use pocketcloud\cloud\http\route\impl\v1\maintenance\MaintenanceAddRoute;
use pocketcloud\cloud\http\route\impl\v1\maintenance\MaintenanceRemoveRoute;
use pocketcloud\cloud\http\route\impl\v1\notification\ListNotificationsRoute;
use pocketcloud\cloud\http\route\impl\v1\notification\NotificationsDisableRoute;
use pocketcloud\cloud\http\route\impl\v1\notification\NotificationsEnableRoute;
use pocketcloud\cloud\http\route\impl\v1\player\KickPlayerRoute;
use pocketcloud\cloud\http\route\impl\v1\player\ListPlayerRoute;
use pocketcloud\cloud\http\route\impl\v1\player\PlayerInfoRoute;
use pocketcloud\cloud\http\route\impl\v1\player\TextPlayerRoute;
use pocketcloud\cloud\http\route\impl\v1\player\TransferPlayerRoute;
use pocketcloud\cloud\http\route\impl\v1\plugin\DisableAllPluginsRoute;
use pocketcloud\cloud\http\route\impl\v1\plugin\DisablePluginRoute;
use pocketcloud\cloud\http\route\impl\v1\plugin\EnableAllPluginsRoute;
use pocketcloud\cloud\http\route\impl\v1\plugin\EnablePluginRoute;
use pocketcloud\cloud\http\route\impl\v1\plugin\ListPluginsRoute;
use pocketcloud\cloud\http\route\impl\v1\plugin\PluginInfoRoute;
use pocketcloud\cloud\http\route\impl\v1\server\ListServersRoute;
use pocketcloud\cloud\http\route\impl\v1\server\ServerInfoRoute;
use pocketcloud\cloud\http\route\impl\v1\server\ServerLogsRoute;
use pocketcloud\cloud\http\route\impl\v1\server\ServerSaveRoute;
use pocketcloud\cloud\http\route\impl\v1\server\ServerSendCommandRoute;
use pocketcloud\cloud\http\route\impl\v1\server\ServerStartRoute;
use pocketcloud\cloud\http\route\impl\v1\server\ServerStopAllRoute;
use pocketcloud\cloud\http\route\impl\v1\server\ServerStopRoute;
use pocketcloud\cloud\http\route\impl\v1\StatsRoute;
use pocketcloud\cloud\http\route\impl\v1\template\CreateTemplateRoute;
use pocketcloud\cloud\http\route\impl\v1\template\EditTemplateRoute;
use pocketcloud\cloud\http\route\impl\v1\template\ListTemplatesRoute;
use pocketcloud\cloud\http\route\impl\v1\template\RemoveTemplateRoute;
use pocketcloud\cloud\http\route\impl\v1\template\TemplateInfoRoute;
use pocketcloud\cloud\http\socket\auth\DefaultAuthentication;
use pocketcloud\cloud\http\version\ApiVersion;
use pocketcloud\cloud\language\Language;
use pocketcloud\cloud\migration\MigratorManager;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\request\RequestManager;
use pocketcloud\cloud\plugin\CloudPluginManager;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\scheduler\AsyncPool;
use pocketcloud\cloud\server\binary\BinaryDownloader;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\server\config\ServerPropertiesGenerator;
use pocketcloud\cloud\server\prepare\ServerPreparator;
use pocketcloud\cloud\software\ServerSoftwareManager;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\thread\ThreadManager;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\update\UpdateChecker;
use pocketcloud\cloud\util\benchmark\Benchmark;
use pocketcloud\cloud\util\benchmark\BenchmarkTimingsSummary;
use pocketcloud\cloud\util\bStats\CloudMetrics;
use pocketcloud\cloud\util\misc\Queue;
use pocketcloud\cloud\library\LibraryManager;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\util\loader\ClassLoader;
use pocketcloud\cloud\util\misc\LoadableList;
use pocketcloud\cloud\util\misc\TickableList;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\ProcessUtils;
use pocketcloud\cloud\util\TerminalUtils;
use pocketcloud\cloud\util\Utils;
use pocketcloud\cloud\util\VersionInfo;
use pocketmine\snooze\SleeperHandler;
use r3pt1s\discord\webhook\message\embed\Embed;
use r3pt1s\discord\webhook\Webhook;
use Ramsey\Uuid\UuidInterface;
use ReflectionException;
use Throwable;
use const pocketcloud\LOG_PATH;
use const pocketcloud\STORAGE_PATH;

final class Server {

    private static ?self $instance = null;

    public bool $running = false {
		get {
			return $this->running;
		}
	}
	public int $tick = 0 {
		get {
			return $this->tick;
		}
	}
	private float $nextTick = 0 {
		get {
			return $this->nextTick;
		}
	}
	private float $startTimestamp = 0 {
		get {
			return $this->startTimestamp;
		}
	}

	private array $tickTimes = [];
    private float $tickTimesSum = 0.0;
    private float $lastTickTime = 0;
    public float $currentTPS = 20.0 {
		get {
			return $this->currentTPS;
		}
	}
	public float $averageTPS = 20.0 {
		get {
			return $this->averageTPS;
		}
	}
	private float $tickUsage = 0.0 {
		get {
			return $this->tickUsage;
		}
	}

	public MainLogger $logger {
		get {
			return $this->logger;
		}
	}
	private Console $console {
		get {
			return $this->console;
		}
	}
	private ScreenManager $screenManager {
		get {
			return $this->screenManager;
		}
	}
	private CommandManager $commandManager {
		get {
			return $this->commandManager;
		}
	}
	private LibraryManager $libraryManager {
		get {
			return $this->libraryManager;
		}
	}
	private MigratorManager $migratorManager;
    private MainConfig $config {
		get {
			return $this->config;
		}
	}
	private ServerSettingsConfig $serverSettingsConfig {
		get {
			return $this->serverSettingsConfig;
		}
	}
	private LogSettingsConfig $logSettingsConfig {
		get {
			return $this->logSettingsConfig;
		}
	}
	public SleeperHandler $sleeperHandler {
		get {
			return $this->sleeperHandler;
		}
	}
	private Queue $startNotificationQueue {
		get {
			return $this->startNotificationQueue;
		}
	}
	private ServerSoftwareManager $softwareManager {
		get {
			return $this->softwareManager;
		}
	}
	private ThreadManager $threadManager {
		get {
			return $this->threadManager;
		}
	}
	private Network $network {
		get {
			return $this->network;
		}
	}
	private HttpServer $httpServer {
		get {
			return $this->httpServer;
		}
	}
	private AsyncPool $asyncPool {
		get {
			return $this->asyncPool;
		}
	}
	private ServerPreparator $serverPreparator {
		get {
			return $this->serverPreparator;
		}
	}
	private RequestManager $requestManager;
    private TemplateManager $templateManager {
		get {
			return $this->templateManager;
		}
	}
	private ServerGroupManager $serverGroupManager {
		get {
			return $this->serverGroupManager;
		}
	}
	private ServerPropertiesGenerator $serverPropertiesGenerator {
		get {
			return $this->serverPropertiesGenerator;
		}
	}
	private CloudServerManager $serverManager {
		get {
			return $this->serverManager;
		}
	}
	private ServerClientCache $serverClientCache {
		get {
			return $this->serverClientCache;
		}
	}
	private TrafficMonitorManager $trafficMonitorManager {
		get {
			return $this->trafficMonitorManager;
		}
	}
	private CloudPluginManager $pluginManager {
		get {
			return $this->pluginManager;
		}
	}
	private UpdateChecker $updateChecker {
		get {
			return $this->updateChecker;
		}
	}

	private UuidInterface $cloudUniqueId {
		get {
			return $this->cloudUniqueId;
		}
	}
	private CloudMetrics $metrics {
		get {
			return $this->metrics;
		}
	}

	public function __construct(private readonly ClassLoader $classLoader) {
        if (self::$instance !== null) {
            throw new \LogicException('Cloud server is already initialized');
        }
        self::$instance = $this;
    }

    public function start(): void {
        if ($this->running) return;
        $this->startTimestamp = microtime(true);
        $this->running = true;
        $this->lastTickTime = microtime(true);

        $this->initBootstrap();
        if (!$this->runMigrations()) return;
        $this->initConfigs();
        if (!$this->initSoftware()) return;
        if (!$this->checkLibraryUpdates()) return;
        if (!$this->checkBridgePlugins()) return;
        $this->initManagers();
        $this->initServices();
        $this->registerTickables();
        $this->printBanner();
        $this->boot();

        $this->tick();
    }

    private function initBootstrap(): void {
        CloudLogger::set($this->logger = new MainLogger(LOG_PATH, false, false));
        ($this->console = new Console())->register();
        ($this->screenManager = new ScreenManager())->resetScreen();
        $this->commandManager = new CommandManager();
        ($this->libraryManager = new LibraryManager())->load();
        $this->migratorManager = new MigratorManager();
    }

    private function runMigrations(): bool {
        CloudLogger::get()->info("Checking for available migrations...");
        if (!$this->migratorManager->checkForAnyMigration()) return true;

        if (($failedMigrations = $this->migratorManager->migrateAll()) > 0) {
            CloudLogger::get()->error("§b{} migration{} failed, shutting down...", $failedMigrations, $failedMigrations == 1 ? "" : "s");
            $this->shutdown();
            return false;
        }

        return true;
    }

    private function initConfigs(): void {
        $this->config = new MainConfig();
        $this->serverSettingsConfig = new ServerSettingsConfig();
        $this->logSettingsConfig = new LogSettingsConfig();
        $this->sleeperHandler = new SleeperHandler();
        $this->startNotificationQueue = Queue::fromType([]);
    }

    private function initSoftware(): bool {
        try {
            ($this->softwareManager = new ServerSoftwareManager())->load();
            $this->softwareManager->downloadAll();
            return true;
        } catch (ReflectionException $e) {
            $this->logger->error("§cFailed to load server software, shutting down...");
            $this->logger->exception($e);
            $this->shutdown();
            return false;
        }
    }

    private function checkLibraryUpdates(): bool {
        CloudLogger::get()->info("Checking for library updates...");
        if ($this->libraryManager->checkForUpdates() > 0) {
            CloudLogger::get()->info("One or more libraries have been updated, please restart the cloud.");
            $this->shutdown();
            return false;
        }

        return true;
    }

    private function checkBridgePlugins(): bool {
        $failures = 0;
        foreach (TemplateType::getAll() as $type) {
            if (!$type->checkBridge()) {
                CloudLogger::get()->info("Starting download for bridge plugin: §b{}", $type->getRelativeBridgeFileLocation());
                if ($type->downloadBridge()) {
                    CloudLogger::get()->success("Successfully downloaded bridge plugin: §b{} §8(§b{}§8)", $type->getRelativeBridgeFileLocation(), $type->getBridgeFileLocation());
                } else {
                    $failures++;
                    CloudLogger::get()->error("Failed to download bridge plugin: §b{} §8(§b{}§8)", $type->getRelativeBridgeFileLocation());
                }
            }
        }

        if ($failures > 0) {
            CloudLogger::get()->warn("At least one bridge plugin download failed, shutting down...");
            $this->shutdown();
            return false;
        }

        return true;
    }

    private function initManagers(): void {
        $this->threadManager = new ThreadManager();
        $this->asyncPool = new AsyncPool();
        $this->serverPreparator = new ServerPreparator();
        $this->requestManager = new RequestManager();
        $this->templateManager = new TemplateManager();
        $this->serverGroupManager = new ServerGroupManager();
        $this->serverPropertiesGenerator = new ServerPropertiesGenerator();
        $this->serverManager = new CloudServerManager();
        $this->serverClientCache = new ServerClientCache();
        $this->trafficMonitorManager = new TrafficMonitorManager();
        $this->pluginManager = new CloudPluginManager();
        $this->updateChecker = new UpdateChecker();
    }

    private function initServices(): void {
        $this->network = new Network(Address::read($this->config->getNetwork()));
        $this->httpServer = HttpServerBuilder::buildFromConfig();
        $this->httpServer->registerVersion(new ApiVersion(ApiVersion::V1, new DefaultAuthentication()));
        $this->registerHttpPaths($this->httpServer);

        $this->cloudUniqueId = Utils::getMachineUniqueId($this->network->getAddress()->getAddress() . $this->network->getAddress()->getPort());
        $this->metrics = new CloudMetrics($this->cloudUniqueId, $this->config);

        $this->updateChecker->checkForUpdates();
        CloudProvider::select();

        if ($this->config->isStartUpDelay()) {
            CloudLogger::get()->info("§bPocket§3Cloud §rwill §astart §rin 3 seconds...");
            sleep(3);
        } else usleep(50 * 1000);

        $this->console->install();

        if (array_any($this->serverSettingsConfig->getBinaries(), fn(string $url, string $templateType) => BinaryDownloader::downloadBinary($url, $templateType) === true)) {
            $this->addStartNotification("§8====== §cATTENTION! §8======", CloudLogLevel::WARN())
                ->addStartNotification("§rNew binaries have been downloaded.", CloudLogLevel::WARN())
                ->addStartNotification("Please make sure that they are NOT corrupted due to issues with PharData.", CloudLogLevel::WARN())
                ->addStartNotification("If they are, please download (& extract) them manually.", CloudLogLevel::WARN())
                ->addStartNotification("Thank you.", CloudLogLevel::WARN());
        }
    }

    private function registerHttpPaths(HttpServer $server): void {
        $server->registerPath(new HealthRoute());
        $server->registerPath(new StatsRoute());

        $server->registerPath(new ListMaintenanceRoute());
        $server->registerPath(new MaintenanceAddRoute());
        $server->registerPath(new MaintenanceRemoveRoute());

        $server->registerPath(new ListNotificationsRoute());
        $server->registerPath(new NotificationsEnableRoute());
        $server->registerPath(new NotificationsDisableRoute());

        $server->registerPath(new ListServersRoute());
        $server->registerPath(new ServerInfoRoute());
        $server->registerPath(new ServerStartRoute());
        $server->registerPath(new ServerStopAllRoute());
        $server->registerPath(new ServerStopRoute());
        $server->registerPath(new ServerSaveRoute());
        $server->registerPath(new ServerSendCommandRoute());
        $server->registerPath(new ServerLogsRoute());

        $server->registerPath(new ListTemplatesRoute());
        $server->registerPath(new TemplateInfoRoute());
        $server->registerPath(new CreateTemplateRoute());
        $server->registerPath(new RemoveTemplateRoute());
        $server->registerPath(new EditTemplateRoute());

        $server->registerPath(new ListPlayerRoute());
        $server->registerPath(new PlayerInfoRoute());
        $server->registerPath(new KickPlayerRoute());
        $server->registerPath(new TransferPlayerRoute());
        $server->registerPath(new TextPlayerRoute());

        $server->registerPath(new ListPluginsRoute());
        $server->registerPath(new PluginInfoRoute());
        $server->registerPath(new EnablePluginRoute());
        $server->registerPath(new DisablePluginRoute());
        $server->registerPath(new EnableAllPluginsRoute());
        $server->registerPath(new DisableAllPluginsRoute());

        $server->registerPath(new ListGroupsRoute());
        $server->registerPath(new GroupInfoRoute());
        $server->registerPath(new CreateGroupRoute());
        $server->registerPath(new RemoveGroupRoute());
        $server->registerPath(new AddTemplatesToGroupRoute());
        $server->registerPath(new RemoveTemplatesFromGroupRoute());
    }

    private function registerTickables(): void {
        TickableList::add(
            $this->requestManager, $this->trafficMonitorManager, $this->serverManager, $this->commandManager, $this->metrics,
            $this->asyncPool, $this->serverClientCache, $this->templateManager, $this->screenManager
        );

        LoadableList::add(
            $this->commandManager,
            $this->templateManager, $this->serverGroupManager,
            $this->serverPropertiesGenerator, $this->serverPreparator,
            $this->pluginManager
        );
    }

    private function printBanner(): void {
        TerminalUtils::clearConsole();
        CloudLogger::get()->setSaveLogs(true);
        CloudLogger::get()->emptyLine()->setFormat("§r{message}")
            ->info("  §bPocket§3Cloud §8- §rA cloud system for §lPocketMine-MP servers§r with §lProxy support§r §8- §b{} §8- §rdeveloped by §b{}", VersionInfo::VERSION . (VersionInfo::BETA ? "§c@BETA" : ""), implode("§8, §b", VersionInfo::DEVELOPERS))
            ->info("  Join our discord for information: §bhttps://discord.gg/3HbPEpaE3T")
            ->emptyLine()->resetFormat();
    }

    private function boot(): void {
        CloudLogger::get()->info("The §bCloud §ris §astarting§r...");

        Language::init();
        $this->network->init();
        $this->httpServer->init();

        LoadableList::loadAll();
        $this->pluginManager->enableAll();

        while (($entry = $this->startNotificationQueue->next()) !== null) {
            if (($entry[0] === CloudLogLevel::DEBUG() && $this->logSettingsConfig->isDebugMode()) || ($entry[0] !== null && $entry[0] !== CloudLogLevel::DEBUG())) {
                CloudLogger::get()->log($entry[0], $entry[1], ...$entry[2]);
            }
        }

        CloudLogger::get()->success("§bCloud §rhas been §astarted§r. §8(§rTook §b" . number_format($time = (microtime(true) - $this->startTimestamp), 3) . "s§8)");
        new CloudStartedEvent($time)->call();

        $this->metrics->getMetrics()->startSubmitting();
    }

    public function crash(): void {
        if (!$this->running) return;
        try {
            OutputManager::reset();
            ScreenManager::getInstance()->resetScreen();
            $crashDump = CrashDump::fromLastestError();
            $filePath = $crashDump->create();

            if (isset($this->logSettingsConfig)) {
                $webhook = LogSettingsConfig::getInstance()->getWebhook();
                if ($webhook instanceof Webhook) {
                    $trace = substr($crashDump->hasTrace() ? $crashDump->stringifyTrace()  : "No trace available", 0, 1000);
                    $webhook->createMessage(false)
                        ->setUsername("PocketCloud Notifications | " . MainConfig::getInstance()->getCloudName())
                        ->setAvatarUrl("https://avatars.githubusercontent.com/u/97796660?s=400&u=a65bced92fb37ce5bafc5f1eff9e2845fe66a9cb&v=4")
                        ->addEmbed(Embed::create()
                            ->setTitle("Notification | Cloud Crashed")
                            ->setDescription("`The cloud crashed.`")
                            ->setColor(0xFF0000)
                            ->addField("**Error Type**", "> " . $crashDump->getType(), true)
                            ->addField("**File**", "> " . $crashDump->getFile() . " (L: " . $crashDump->getLine() . ")", true)
                            ->addField("**Message**", "> " . $crashDump->getMessage())
                            ->addField("**Trace**", "```php\n" . $trace . "\n```")
                            ->setTimestamp(time())
                        )
                        ->send();
                }
            }

            CloudLogger::get()->error("§cAn error has occurred and caused the Cloud to crash entirely.");
            CloudLogger::get()->error("§cA crashdump has been created.");
            CloudLogger::get()->error("§c(§b{}§c)", $filePath);
        } catch (Throwable $e) {
            CloudLogger::get()->error("§cFailed to create crashdump§8: §e" . $e->getMessage());
            if (isset($this->logSettingsConfig)) {
                $webhook = LogSettingsConfig::getInstance()->getWebhook();
                if ($webhook instanceof Webhook) {
                    $webhook->createMessage(false)
                        ->setUsername("PocketCloud Notifications")
                        ->setAvatarUrl("https://avatars.githubusercontent.com/u/97796660?s=400&u=a65bced92fb37ce5bafc5f1eff9e2845fe66a9cb&v=4")
                        ->addEmbed(Embed::create()
                            ->setTitle("Notification | Cloud Crashed")
                            ->setDescription("`The cloud crashed while creating a crash dump.`")
                            ->setColor(0xFF0000)
                            ->addField("**Message**", "> " . $e->getMessage())
                            ->setTimestamp(time())
                        )
                        ->send();
                }
            }
        }

        $this->shutdown();
        echo "--- Uptime: " . round($this->getUptime(), 3) . "s - PocketCloud has crashed, waiting 60s before completely killing the process. ---" . PHP_EOL;
        sleep(60);
        @ProcessUtils::kill(getmypid());
        exit(1);
    }

    public function shutdown(): void {
        if (!$this->running) return;

        OutputManager::reset();
        ScreenManager::getInstance()->resetScreen();

        CloudLogger::get()->info("§cShutting down §bPocket§3Cloud§r...");
        $this->running = false;

        if (isset($this->config)) {
            if ($this->config->isWriteTimingsOnShutdown()) {
                CloudLogger::get()->info("Writing timings... §8(§b{}§8)", $timingsPath = STORAGE_PATH . "latest_timings.txt");

                @unlink($timingsPath);
                $file = fopen($timingsPath, "w");
                /** @var BenchmarkTimingsSummary $summary */
                foreach (Benchmark::getSummary() as $summary) {
                    fwrite($file, $summary->format() . PHP_EOL);
                }

                fclose($file);
            }
        }

        if (isset($this->serverManager)) $this->serverManager->stopAll(true);
        if (isset($this->network)) $this->network->close();
        if (isset($this->httpServer)) $this->httpServer->stop();
        if (isset($this->serverPreparator)) $this->serverPreparator->stop();
        if (isset($this->console)) $this->console->remove();
    }

    public function tick(): void {
        $this->nextTick = microtime(true);
        ProcessUtils::startCpuRetrieveCycle();
        while ($this->running) {
            $tickStart = microtime(true);
            if (($tickStart - $this->nextTick) < -0.025) {
                $this->sleeperHandler->sleepUntil($this->nextTick);
                continue;
            }

            Benchmark::startTiming("full_cloud_tick");
            $this->tick++;
            TickableList::tickAll($this->tick);

            Benchmark::stopTiming("full_cloud_tick");

            $tickWorkEnd = microtime(true);
            $this->console->readLine();
            if (($this->tick % 40) == 0) ProcessUtils::restartCpuRetrieveCycle();

            if (($this->nextTick - $tickStart) < -1) {
                $this->nextTick = $tickStart;
            } else {
                $this->nextTick += 1 / 20;
            }

            $this->updatePerformanceMetrics($tickStart, $tickWorkEnd);

            $this->sleeperHandler->sleepUntil($this->nextTick);
        }
    }

    private function updatePerformanceMetrics(float $tickStart, float $tickWorkEnd): void {
        $timeSinceLastTick = $tickStart - $this->lastTickTime;
        $this->lastTickTime = $tickStart;

        if ($timeSinceLastTick > 0) {
            $this->currentTPS = min(20.0, 1.0 / $timeSinceLastTick);
        }

        $this->tickTimesSum += $timeSinceLastTick;
        $this->tickTimes[] = $timeSinceLastTick;
        if (count($this->tickTimes) > 20) {
            $this->tickTimesSum -= array_shift($this->tickTimes);
        }

        $avgTickTime = $this->tickTimesSum / count($this->tickTimes);
        $this->averageTPS = $avgTickTime > 0 ? min(20.0, 1.0 / $avgTickTime) : 20.0;

        $this->tickUsage = min(100.0, ($tickWorkEnd - $tickStart) * 2000.0);
    }

    public function addStartNotification(string $logMessage, ?CloudLogLevel $logLevel = null, mixed... $params): self {
        if ($this->tick > 0) {
            if (($logLevel === CloudLogLevel::DEBUG() && $this->logSettingsConfig->isDebugMode()) || ($logLevel !== null && $logLevel !== CloudLogLevel::DEBUG())) {
                CloudLogger::get()->log($logLevel, $logMessage, $params);
            }
        }
        else $this->startNotificationQueue->add([$logLevel ?? CloudLogLevel::INFO(), $logMessage, $params]);
        return $this;
    }

	public function getTickPerformanceMetrics(): array {
        return [
            "current_tps" => $this->currentTPS,
            "average_tps" => $this->averageTPS,
            "tick_usage" => $this->tickUsage
        ];
    }

	public function getUptime(): float {
        if ($this->startTimestamp <= 0) return 0;
        return microtime(true) - $this->startTimestamp;
    }

	public function getClassLoader(): ClassLoader {
        return $this->classLoader;
    }

    public static function getInstance(): ?self {
        return self::$instance;
    }
}