package de.pocketcloud.cloud;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.CloudAPIHolder;
import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.config.IEnvironmentConfig;
import de.pocketcloud.api.executor.IPlayerExecutor;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.api.service.ServiceRegistry;
import de.pocketcloud.cloud.config.CloudEnvironmentConfig;
import de.pocketcloud.cloud.config.LogSettingsConfig;
import de.pocketcloud.cloud.config.MainConfig;
import de.pocketcloud.cloud.config.ServerSettingsConfig;
import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.CloudShutdownHook;
import de.pocketcloud.cloud.console.command.CommandManager;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.output.OutputManager;
import de.pocketcloud.cloud.console.screen.ScreenManager;
import de.pocketcloud.cloud.event.EventManager;
import de.pocketcloud.cloud.event.impl.cloud.CloudReadyEvent;
import de.pocketcloud.cloud.http.HttpServer;
import de.pocketcloud.cloud.http.traffic.HttpTrafficMonitor;
import de.pocketcloud.cloud.language.LanguageManager;
import de.pocketcloud.cloud.load.Loader;
import de.pocketcloud.cloud.network.NetworkNettyServer;
import de.pocketcloud.cloud.network.client.ServerClientCache;
import de.pocketcloud.cloud.network.packet.PacketRegistry;
import de.pocketcloud.cloud.notification.NotificationService;
import de.pocketcloud.cloud.player.CloudPlayerManager;
import de.pocketcloud.cloud.player.executor.CloudPlayerExecutor;
import de.pocketcloud.cloud.plugin.CloudPluginManager;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.server.CloudServerManager;
import de.pocketcloud.cloud.server.config.ServerPropertiesGenerator;
import de.pocketcloud.cloud.server.crash.CrashHandlerRegistry;
import de.pocketcloud.cloud.server.library.LibraryManager;
import de.pocketcloud.cloud.server.software.ServerSoftwareManager;
import de.pocketcloud.cloud.server.software.SoftwareService;
import de.pocketcloud.cloud.template.TemplateManager;
import de.pocketcloud.cloud.template.group.ServerGroupManager;
import de.pocketcloud.cloud.tick.Ticker;
import de.pocketcloud.cloud.update.UpdateChecker;
import de.pocketcloud.cloud.util.PerformanceStats;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.cloud.util.VersionInfo;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.cloud.util.benchmark.BenchmarkTiming;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.common.util.NumberUtils;
import de.pocketcloud.network.request.RequestManager;
import de.pocketcloud.network.traffic.TrafficMonitorManager;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Getter
@Accessors(fluent = true)
public final class PocketCloud implements CloudAPI {

    @Getter
    private static PocketCloud instance = null;

    private boolean running;
    private boolean firstRun = false;
    private boolean hasStopped = false;
    private Instant startTime = null;

    private final List<Map<String, Object>> startNotifications = new ArrayList<>();
    private boolean startNotificationsFlushed = false;

    private final PerformanceStats performanceStats = new PerformanceStats();
    private final ServiceRegistry services = new ServiceRegistry();

    public PocketCloud() throws IOException {
        instance = this;
        running = true;
        CloudAPIHolder.setInstance(this);

        int version = Runtime.version().feature();
        if (version < 22) {
            System.err.println("You need Java 22 or higher to be able to use PocketCloud.");
            System.err.println("You currently use Java " + version + ".");
            System.err.println("Update your Java version.");
            System.exit(0);
            return;
        }

        System.out.println("Cleaning tmp/ folder...");
        FileUtils.removeDirectory(PocketCloudPaths.tmp().asPath());

        createDirectories();

        System.out.println("Checking for previous used versions...");
        Path firstRunPath = PocketCloudPaths.storage().with(".first_run").asPath();
        if (!Files.exists(firstRunPath)) {
            firstRun = true;
            try {
                System.out.println("You are using an incompatible folder structure of PocketCloud.");
                System.out.println("All your important files will be backed up inside the new storage/backups/.");
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                String backupId = "backup-" + format.format(new Date());
                Path backupPath = PocketCloudPaths.storage().backups().with(backupId).asPath();
                FileUtils.createDir(backupPath);

                List<String> oldFolders = List.of(
                        "servers",
                        "templates",
                        "software",
                        "groups",
                        "storage/software",
                        "storage/plugins",
                        "storage/staticServers",
                        "storage/config.json",
                        "storage/log_settings.yml",
                        "storage/server_settings.yml",
                        "storage/binaries",
                        "storage/libraries",
                        "storage/inGame"
                );

                for (String name : oldFolders) {
                    Path folder = Path.of(name);

                    if (Files.exists(folder)) {
                        FileUtils.copyDirectory(
                                folder,
                                backupPath.resolve(name),
                                Set.of()
                        );

                        FileUtils.removeDirectory(folder);
                    }
                }

                createDirectories();

                FileUtils.filePutContents(firstRunPath, String.valueOf(System.currentTimeMillis()));
            } catch (Exception e) {
                System.err.println("An error occurred during the backup process.");
                e.printStackTrace();
                return;
            }
        }

        services.register(Ticker.class, new Ticker());

        Benchmark.startTiming("cloud_start");

        services.register(Loader.class, new Loader());
        services.register(MainConfig.class, loadYmlConfig(MainConfig.class, PocketCloudPaths.storage().configs().with("config.yml").asPath()));
        services.register(LogSettingsConfig.class, loadYmlConfig(LogSettingsConfig.class, PocketCloudPaths.storage().configs().with("log_settings.yml").asPath()));
        services.register(ServerSettingsConfig.class, loadYmlConfig(ServerSettingsConfig.class, PocketCloudPaths.storage().configs().with("server_settings.yml").asPath()));
        services.register(CloudEnvironmentConfig.class, new CloudEnvironmentConfig());
        services.register(NotificationService.class, new NotificationService());

        Thread.setDefaultUncaughtExceptionHandler((_, throwable) -> {
            CloudLogger.get().exception(throwable);
            shutdown();
        });

        services.register(CloudConsole.class, new CloudConsole())
                .install()
                .start();

        services.register(ScreenManager.class, new ScreenManager())
                .reset();

        services.register(LanguageManager.class, new LanguageManager());
        services.register(CommandManager.class, new CommandManager());
        services.register(SoftwareService.class, new SoftwareService());
        services.register(ServerSoftwareManager.class, new ServerSoftwareManager());
        services.register(LibraryManager.class, new LibraryManager());
        services.register(ServerPropertiesGenerator.class, new ServerPropertiesGenerator());
        services.register(TemplateManager.class, new TemplateManager());
        services.register(ServerGroupManager.class, new ServerGroupManager());
        services.register(CloudServerManager.class, new CloudServerManager());
        services.register(CloudPlayerExecutor.class, new CloudPlayerExecutor());
        services.register(CloudPlayerManager.class, new CloudPlayerManager());
        services.register(NetworkNettyServer.class, new NetworkNettyServer(config().network().socketAddress(), config().network().encryption(), config().network().packetSizeLimit()));
        services.register(HttpServer.class, new HttpServer(config().httpServer().socketAddress(), config().httpServer().authKey()));
        services.register(RequestManager.class, new RequestManager());
        services.register(PacketRegistry.class, new PacketRegistry());
        services.register(ServerClientCache.class, new ServerClientCache());
        services.register(TrafficMonitorManager.class, new TrafficMonitorManager());
        services.register(CloudPluginManager.class, new CloudPluginManager());
        services.register(EventManager.class, new EventManager());
        services.register(CrashHandlerRegistry.class, new CrashHandlerRegistry());

        traffic().registerTrafficMonitorType(HttpTrafficMonitor.class, "http");

        for (Object obj : services.getAll().values()) {
            if (obj instanceof Tickable tickable) {
                ticker().register(tickable);
            }

            if (obj instanceof Loadable loadable) {
                loader().register(loadable);
            }
        }

        loader().preloadAll();

        printBanner();
        CloudLogger.get().info("The §bCloud §ris §astarting§r...");

        if (config().checkForUpdates()) {
            UpdateChecker.check(VersionInfo.VERSION)
                    .thenSuccess(res -> {
                        if (res.updateAvailable()) {
                            Duration duration = Duration.between(res.updateReleasedAt(), Instant.now());
                            long days = duration.toDaysPart();
                            int hours = duration.toHoursPart();
                            int minutes = duration.toMinutesPart();
                            int seconds = duration.toSecondsPart();

                            List<String> timeParts = new ArrayList<>();
                            if (days > 0) timeParts.add(days + " day" + (days == 1 ? "" : "s"));
                            if (hours > 0) timeParts.add(hours + " hour" + (hours == 1 ? "" : "s"));
                            if (minutes > 0) timeParts.add(minutes + " minute" + (minutes == 1 ? "" : "s"));
                            if (seconds > 0 || timeParts.isEmpty()) timeParts.add(seconds + " second" + (seconds == 1 ? "" : "s"));

                            appendStartNotification("§cUpdate for §bPocket§3Cloud §cis available!", CloudLogLevel.WARN);
                            appendStartNotification("§cYou are currently running on version §b{}§c.", CloudLogLevel.WARN, res.currentVersion());
                            appendStartNotification("§cThe latest version §b{} §cwas released §e{} §cago.", CloudLogLevel.WARN, res.latestVersion(), String.join("§8, §e", timeParts));
                            appendStartNotification("§cDownload the latest update here§8: §ehttps://github.com/PocketCloudSystem/PocketCloud/releases/tag/latest-core", CloudLogLevel.WARN);
                        } else appendStartNotification("§bCloud §ris §aup-to-date§r!", CloudLogLevel.INFO);
                    })
                    .failure(ex -> appendStartNotification("§cFailed to check for updates: §e{}", CloudLogLevel.ERROR, ex.getMessage()));
        }

        CloudProvider.select();

        clearAndFlushStartNotifications();

        loader().loadAll();

        if (softwares().getAll().isEmpty()) {
            CloudLogger.get().warn("No software found, therefore no server can be started.");
        }

        network().start();
        if (config().httpServer().enabled()) httpServer().start();

        Runtime.getRuntime().addShutdownHook(new CloudShutdownHook());

        BenchmarkTiming result = Benchmark.stopTiming("cloud_start");
        CloudLogger.get().info("§bCloud §rhas been §astarted§r. §8(§rTook §b{}s§8)", NumberUtils.formatNumber(result.duration() / 1000, 3));
        startTime = Instant.now();
        new CloudReadyEvent(startTime).call();

        ticker().tick();
    }

    public void clearAndFlushStartNotifications() {
        if (startNotificationsFlushed) return;
        startNotificationsFlushed = true;
        for (Map<String, Object> map : startNotifications) {
            String message = map.get("message").toString();
            CloudLogLevel level = (CloudLogLevel) map.get("level");
            Object[] args = (Object[]) map.get("args");
            CloudLogger.get().log(level, message, args);
        }
    }

    private <T extends OkaeriConfig> T loadYmlConfig(Class<T> clazz, Path filePath) {
        return ConfigManager.create(clazz, config -> {
            config.withConfigurer(new YamlSnakeYamlConfigurer());
            config.withBindFile(filePath);
            config.withRemoveOrphans(true);
            config.saveDefaults();
            reloadYmlConfig(config);
        });
    }

    private <T extends OkaeriConfig> void reloadYmlConfig(T config) {
        config.load(true);
        if (config instanceof ICloudConfig cloudConfig) {
            cloudConfig.validate();
            cloudConfig.apply();
        }
    }

    public PocketCloud appendStartNotification(String message, CloudLogLevel level, Object... args) {
        if (startNotificationsFlushed) {
            CloudLogger.get().log(level, message, args);
        } else {
            startNotifications.add(Map.ofEntries(
                    Map.entry("message", message),
                    Map.entry("level", level),
                    Map.entry("args", args)
            ));
        }

        return this;
    }

    private void printBanner() {
        console().clear();
        CloudLogger.get().emptyLine()
                .withoutFormat("  §bPocket§3Cloud §8- §rA cloud system for §lPocketMine-MP servers§r with §lProxy support§r §8- §b{} §8- §rdeveloped by §b{}", VersionInfo.VERSION + (VersionInfo.BETA ? "§c@BETA" : ""), String.join("§8, §b", VersionInfo.DEVELOPERS))
                .withoutFormat("  Join our discord for information: §bhttps://discord.gg/3HbPEpaE3T")
                .emptyLine();
    }

    private void createDirectories() {
        FileUtils.createDirs(PocketCloudPaths.ALL_DIRECTORIES.stream().map(Path::of).toArray(Path[]::new));
    }

    public void reload() {
        if (!running || hasStopped) return;
        if (loader().isReloading()) return;
        CloudLogger.get().info("Reloading...");
        CloudLogger.get().warn("§cNOTE: §rNot everything is reloadable. To achieve the best outcome, restarting the cloud would be the better option.");
        try {
            boolean httpServerBeforeReload = config().httpServer().enabled();
            reloadYmlConfig(config());
            reloadYmlConfig(logSettingsConfig());
            reloadYmlConfig(serverSettingsConfig());
            loader().reload();

            if (!httpServerBeforeReload && config().httpServer().enabled()) {
                httpServer().start();
            } else if (httpServerBeforeReload && !config().httpServer().enabled()) {
                httpServer().close();
            }

            CloudLogger.get().success("Reload complete.");
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to reload", e);
            shutdown();
        }
    }

    public void shutdown() {
        if (!running || hasStopped) return;

        hasStopped = true;
        running = false;

        try {
            shutdown0();
        } catch (Exception e) {
            CloudLogger.get().exception("Unable to shutdown", e);
        }

        System.exit(0);
    }

    private void shutdown0() {
        OutputManager.reset();

        screens().reset();

        CloudLogger.get().info("Shutting down...");

        CloudLogger.get().info("§cStopping §rall servers...");
        servers().stopAllAndWait(10 * 1000);

        loader().unloadAll();
        network().close();

        if (config().writeTimingsOnShutdown()) {
            Path path = PocketCloudPaths.storage().timings().with("latest_timings.txt").asPath();
            logger().info("Writing timings into §b{}§r...", path.toAbsolutePath().toString());
            Benchmark.writeTimings(path, true);
        }

        CloudLogger.get().success("§cStopped §rthe §bcloud§r.");
        console().uninstall();
    }

    public Duration uptime() {
        if (startTime == null) return Duration.ZERO;
        return Duration.between(startTime, Instant.now());
    }

    public long currentTick() {
        return ticker().tickCounter();
    }

    public Ticker ticker() {
        return services.get(Ticker.class);
    }

    public Loader loader() {
        return services.get(Loader.class);
    }

    public MainConfig config() {
        return services.get(MainConfig.class);
    }

    public LogSettingsConfig logSettingsConfig() {
        return services.get(LogSettingsConfig.class);
    }

    public ServerSettingsConfig serverSettingsConfig() {
        return services.get(ServerSettingsConfig.class);
    }

    public NotificationService notifications() {
        return services.get(NotificationService.class);
    }

    public CloudConsole console() {
        return services.get(CloudConsole.class);
    }

    public ScreenManager screens() {
        return services.get(ScreenManager.class);
    }

    public CommandManager commands() {
        return services.get(CommandManager.class);
    }

    public ServerSoftwareManager softwares() {
        return services.get(ServerSoftwareManager.class);
    }

    public SoftwareService software() {
        return services.get(SoftwareService.class);
    }

    public LibraryManager libraries() {
        return services.get(LibraryManager.class);
    }

    public ServerPropertiesGenerator properties() {
        return services.get(ServerPropertiesGenerator.class);
    }

    public CrashHandlerRegistry crashHandlers() {
        return services.get(CrashHandlerRegistry.class);
    }

    @Override
    public TemplateManager templates() {
        return services.get(TemplateManager.class);
    }

    @Override
    public LanguageManager language() {
        return services.get(LanguageManager.class);
    }

    @Override
    public ILogger logger() {
        return CloudLogger.get();
    }

    public ServerGroupManager serverGroups() {
        return services.get(ServerGroupManager.class);
    }

    @Override
    public CloudServerManager servers() {
        return services.get(CloudServerManager.class);
    }

    @Override
    public IPlayerExecutor playerExecutor() {
        return services.get(CloudPlayerExecutor.class);
    }

    @Override
    public CloudPlayerManager players() {
        return services.get(CloudPlayerManager.class);
    }

    public NetworkNettyServer network() {
        return services.get(NetworkNettyServer.class);
    }

    public HttpServer httpServer() {
        return services.get(HttpServer.class);
    }

    public RequestManager requests() {
        return services.get(RequestManager.class);
    }

    @Override
    public PacketRegistry packets() {
        return services.get(PacketRegistry.class);
    }

    public ServerClientCache clients() {
        return services.get(ServerClientCache.class);
    }

    public TrafficMonitorManager traffic() {
        return services.get(TrafficMonitorManager.class);
    }

    public CloudPluginManager plugins() {
        return services.get(CloudPluginManager.class);
    }

    public EventManager events() {
        return services.get(EventManager.class);
    }

    @Override
    public IEnvironmentConfig environmentConfig() {
        return services.get(CloudEnvironmentConfig.class);
    }
}