<?php

namespace pocketcloud\cloud;

use Phar;
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
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\misc\Queue;
use pocketcloud\cloud\library\LibraryManager;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\util\loader\ClassLoader;
use pocketcloud\cloud\util\misc\LoadableList;
use pocketcloud\cloud\util\misc\TickableList;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\PathUtils;
use pocketcloud\cloud\util\ProcessUtils;
use pocketcloud\cloud\util\TerminalUtils;
use pocketcloud\cloud\util\Utils;
use pocketcloud\cloud\util\VersionInfo;
use pocketmine\snooze\SleeperHandler;
use r3pt1s\discord\webhook\message\embed\Embed;
use r3pt1s\discord\webhook\Webhook;
use Ramsey\Uuid\UuidInterface;
use ReflectionException;
use RuntimeException;
use Throwable;
use const pocketcloud\BACKUPS_PATH;
use const pocketcloud\BINARIES_PATH;
use const pocketcloud\CLOUD_PATH;
use const pocketcloud\CRASHES_PATH;
use const pocketcloud\GLOBAL_TEMPLATES_PATH;
use const pocketcloud\IN_GAME_PATH;
use const pocketcloud\INTERNAL_PATH;
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
    private float $nextTick = 0;
    private float $startTimestamp = 0;

    private array $tickTimes = [];
    private float $tickTimesSum = 0.0;
    private float $lastTickTime = 0;
    private float $currentTPS = 20.0;
    private float $averageTPS = 20.0;
    private float $tickUsage = 0.0;

    private MainLogger $logger;
    private Console $console;
    private ScreenManager $screenManager;
    private CommandManager $commandManager;
    private LibraryManager $libraryManager;
    private MigratorManager $migratorManager;
    private MainConfig $config;
    private ServerSettingsConfig $serverSettingsConfig;
    private LogSettingsConfig $logSettingsConfig;
    private SleeperHandler $sleeperHandler;
    private Queue $startNotificationQueue;
    private ServerSoftwareManager $softwareManager;
    private ThreadManager $threadManager;
    private Network $network;
    private HttpServer $httpServer;
    private AsyncPool $asyncPool;
    private ServerPreparator $serverPreparator;
    private RequestManager $requestManager;
    private TemplateManager $templateManager;
    private ServerGroupManager $serverGroupManager;
    private ServerPropertiesGenerator $serverPropertiesGenerator;
    private CloudServerManager $serverManager;
    private ServerClientCache $serverClientCache;
    private TrafficMonitorManager $trafficMonitorManager;
    private CloudPluginManager $pluginManager;
    private UpdateChecker $updateChecker;

    private UuidInterface $cloudUniqueId;
    private CloudMetrics $metrics;

    public function __construct(private readonly ClassLoader $classLoader) {
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
            $this->getLogger()->error("§cFailed to load server software, shutting down...");
            $this->getLogger()->exception($e);
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

    public function getCurrentTPS(): float {
        return $this->currentTPS;
    }

    public function getAverageTPS(): float {
        return $this->averageTPS;
    }

    public function getTickUsage(): float {
        return $this->tickUsage;
    }

    public function getTickPerformanceMetrics(): array {
        return [
            "current_tps" => $this->currentTPS,
            "average_tps" => $this->averageTPS,
            "tick_usage" => $this->tickUsage
        ];
    }

    public function isRunning(): bool {
        return $this->running;
    }

    public function getTick(): int {
        return $this->tick;
    }

    public function getNextTick(): float {
        return $this->nextTick;
    }

    public function getStartTimestamp(): float {
        return $this->startTimestamp;
    }

    public function getUptime(): float {
        if ($this->startTimestamp <= 0) return 0;
        return microtime(true) - $this->startTimestamp;
    }

    public function getLogger(): MainLogger {
        return $this->logger;
    }

    public function getConsole(): Console {
        return $this->console;
    }

    public function getScreenManager(): ScreenManager {
        return $this->screenManager;
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

    public function getServerSettingsConfig(): ServerSettingsConfig {
        return $this->serverSettingsConfig;
    }

    public function getLogSettingsConfig(): LogSettingsConfig {
        return $this->logSettingsConfig;
    }

    public function getSoftwareManager(): ServerSoftwareManager {
        return $this->softwareManager;
    }

    public function getThreadManager(): ThreadManager {
        return $this->threadManager;
    }

    public function getNetwork(): Network {
        return $this->network;
    }

    public function getHttpServer(): HttpServer {
        return $this->httpServer;
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

    public function getServerClientCache(): ServerClientCache {
        return $this->serverClientCache;
    }

    public function getTrafficMonitorManager(): TrafficMonitorManager {
        return $this->trafficMonitorManager;
    }

    public function getPluginManager(): CloudPluginManager {
        return $this->pluginManager;
    }

    public function getUpdateChecker(): UpdateChecker {
        return $this->updateChecker;
    }

    public function getCloudUniqueId(): UuidInterface {
        return $this->cloudUniqueId;
    }

    public function getMetrics(): CloudMetrics {
        return $this->metrics;
    }

    public function getClassLoader(): ClassLoader {
        return $this->classLoader;
    }

    public static function getInstance(): ?self {
        return self::$instance;
    }
}

error_reporting(-1);

$autoloadPath = dirname(__FILE__, 2) . "/vendor/autoload.php";
require_once $autoloadPath;

checkPlatform();

define("pocketcloud\VENDOR_AUTOLOAD_PATH", $autoloadPath);
define("pocketcloud\IS_PHAR", Phar::running() !== "");
define("pocketcloud\SOURCE_PATH", __DIR__ . "/");

define("pocketcloud\CLOUD_PATH", (IS_PHAR ?
    str_replace("phar://", "", dirname(__DIR__, 2) . "/") :
    dirname(__DIR__) . "/"
));

define("pocketcloud\STORAGE_PATH", PathUtils::join(CLOUD_PATH, "storage") . "/");
define("pocketcloud\BACKUPS_PATH", PathUtils::join(STORAGE_PATH, "backups") . "/");
define("pocketcloud\INTERNAL_PATH", PathUtils::join(STORAGE_PATH, "internal") . "/");
define("pocketcloud\CRASHES_PATH", PathUtils::join(STORAGE_PATH, "crashes") . "/");
define("pocketcloud\SERVER_CRASHES_PATH", PathUtils::join(CRASHES_PATH, "servers") . "/");
define("pocketcloud\BINARIES_PATH", PathUtils::join(STORAGE_PATH, "binaries") . "/");
define("pocketcloud\LIBRARIES_PATH", PathUtils::join(STORAGE_PATH, "libraries") . "/");
define("pocketcloud\PLUGINS_PATH", PathUtils::join(STORAGE_PATH, "plugins") . "/");
define("pocketcloud\SOFTWARE_PATH", PathUtils::join(STORAGE_PATH, "software") . "/");
define("pocketcloud\IN_GAME_PATH", PathUtils::join(STORAGE_PATH, "inGame") . "/");
define("pocketcloud\STATIC_SERVERS_PATH", PathUtils::join(STORAGE_PATH, "staticServers") . "/");
define("pocketcloud\LOG_PATH", PathUtils::join(STORAGE_PATH, "cloud.log"));
define("pocketcloud\TEMP_PATH", PathUtils::join(CLOUD_PATH, "tmp") . "/");
define("pocketcloud\TEMPLATES_PATH", PathUtils::join(CLOUD_PATH, "templates") . "/");
define("pocketcloud\GLOBAL_TEMPLATES_PATH", PathUtils::join(TEMPLATES_PATH, "global") . "/");
define("pocketcloud\SERVER_GROUPS_PATH", PathUtils::join(CLOUD_PATH, "groups") . "/");
define("pocketcloud\FIRST_RUN", !file_exists(STORAGE_PATH . "config.json"));

foreach ([
    STORAGE_PATH, BACKUPS_PATH, INTERNAL_PATH, CRASHES_PATH, SERVER_CRASHES_PATH, BINARIES_PATH, LIBRARIES_PATH, PLUGINS_PATH, SOFTWARE_PATH, IN_GAME_PATH, LOG_PATH,
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

$cloud = new PocketCloud($classLoader);
$cloud->start();

if (ThreadManager::getInstance()->stopAll() > 0) {
    CloudLogger::get()->warn("Some threads crashed while trying to stop them, force-kill of the process...");
    @ProcessUtils::kill(getmypid());
}

releaseLockFile($lockFile);

exit(0);

function checkRunning(?int &$pid = null): bool {
    if (!file_exists(INTERNAL_PATH)) return false;
    $file = fopen(INTERNAL_PATH . "cloud.lock", "a+b");
    if ($file === false) return false;
    if (!flock($file, LOCK_EX | LOCK_NB)) {
        flock($file, LOCK_SH);
        $processId = stream_get_contents($file);
        if (preg_match('/^\d+$/', $processId) === 1) $pid = $processId;
        return true;
    }

    return false;
}

function createLockFile() {
    $file = fopen(INTERNAL_PATH . "cloud.lock", "a+b");
    if ($file === false) throw new RuntimeException("Failed to create cloud.lock file");
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
    unlink(INTERNAL_PATH . "cloud.lock");
}

function checkPlatform(): void {
    if (PHP_OS_FAMILY == "Windows") {
        die("[ERROR] You can't use PocketCloud on a windows machine." . PHP_EOL);
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

    if (!TerminalUtils::checkCommand("java")) {
        $messages[] = "[ERROR] Java is required. Please install at least Java 17.";
    }

    if (count($messages) > 0) {
        foreach ($messages as $message) {
            echo $message . PHP_EOL;
        }
        exit;
    }
}