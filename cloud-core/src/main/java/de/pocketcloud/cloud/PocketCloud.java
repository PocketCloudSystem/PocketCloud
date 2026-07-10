package de.pocketcloud.cloud;

import de.pocketcloud.cloud.config.LogSettingsConfig;
import de.pocketcloud.cloud.config.MainConfig;
import de.pocketcloud.cloud.config.ServerSettingsConfig;
import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.CloudShutdownHook;
import de.pocketcloud.cloud.console.command.CommandManager;
import de.pocketcloud.cloud.console.log.CloudLogLevel;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.log.def.MainLogger;
import de.pocketcloud.cloud.console.output.OutputManager;
import de.pocketcloud.cloud.console.screen.ScreenManager;
import de.pocketcloud.cloud.event.EventManager;
import de.pocketcloud.cloud.http.HttpServer;
import de.pocketcloud.cloud.load.Loader;
import de.pocketcloud.cloud.network.NetworkNettyServer;
import de.pocketcloud.cloud.network.client.ServerClientCache;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.cloud.network.packet.impl.*;
import de.pocketcloud.cloud.network.packet.impl.request.*;
import de.pocketcloud.cloud.network.packet.impl.request.client.CommandExecuteRequestPacket;
import de.pocketcloud.cloud.network.packet.impl.response.*;
import de.pocketcloud.cloud.network.packet.impl.response.client.CommandExecuteResponsePacket;
import de.pocketcloud.common.util.NumberUtils;
import de.pocketcloud.network.packet.PacketPool;
import de.pocketcloud.cloud.network.request.RequestManager;
import de.pocketcloud.cloud.player.CloudPlayerManager;
import de.pocketcloud.cloud.plugin.CloudPluginManager;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.server.CloudServerManager;
import de.pocketcloud.cloud.server.config.ServerPropertiesGenerator;
import de.pocketcloud.cloud.server.library.LibraryManager;
import de.pocketcloud.cloud.server.software.ServerSoftwareManager;
import de.pocketcloud.cloud.template.TemplateManager;
import de.pocketcloud.cloud.template.group.ServerGroupManager;
import de.pocketcloud.cloud.tick.Ticker;
import de.pocketcloud.cloud.util.*;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.cloud.util.benchmark.BenchmarkTiming;
import de.pocketcloud.network.traffic.TrafficMonitorManager;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Accessors(fluent = true)
public final class PocketCloud {

    public static void main(String[] args) throws IOException {
        new PocketCloud();
    }

    @Getter
    private static PocketCloud instance = null;

    private boolean running;
    private boolean hasStopped = false;
    private long startTime = 0;

    private final List<Map<String, Object>> startNotifications = new ArrayList<>();

    private final PerformanceStats performanceStats = new PerformanceStats();
    private Ticker ticker = null;
    private Loader loader = null;

    private MainConfig config = null;
    private MainLogger logger = null;
    private LogSettingsConfig logSettingsConfig = null;
    private CloudConsole console = null;
    private ScreenManager screenManager = null;
    private CommandManager commandManager = null;
    private ServerSoftwareManager serverSoftwareManager = null;
    private LibraryManager libraryManager = null;
    private ServerPropertiesGenerator propertiesGenerator = null;
    private ServerSettingsConfig serverSettingsConfig = null;
    private TemplateManager templateManager = null;
    private ServerGroupManager serverGroupManager = null;
    private CloudServerManager serverManager = null;
    private CloudPlayerManager playerManager = null;
    private NetworkNettyServer network = null;
    private HttpServer httpServer = null;
    private RequestManager requestManager = null;
    private PacketPool packetPool = null;
    private ServerClientCache clientCache = null;
    private TrafficMonitorManager trafficMonitorManager = null;
    private CloudPluginManager pluginManager = null;
    private EventManager eventManager = null;

    public PocketCloud() throws IOException {
        instance = this;
        running = true;

        int version = Runtime.version().feature();
        if (version < 22) {
            System.err.println("You need Java 22 or higher to be able to use PocketCloud.");
            System.err.println("You currently use Java " + version + ".");
            System.err.println("Update your Java version.");
            System.exit(0);
            return;
        }

        ticker = new Ticker();
        loader = new Loader();

        Benchmark.startTiming("cloud_start");

        createDirectories();

        config = new MainConfig();
        CloudLogger.set(logger = (MainLogger) CloudLogger.tmp("storage/cloud.log"));
        logSettingsConfig = new LogSettingsConfig();

        Thread.setDefaultUncaughtExceptionHandler((_, throwable) -> {
            CloudLogger.get().exception(throwable);
            shutdown();
        });

        console = new CloudConsole();
        console.install();
        console.start();

        screenManager = new ScreenManager();
        screenManager.reset();

        commandManager = new CommandManager();
        serverSoftwareManager = new ServerSoftwareManager();
        libraryManager = new LibraryManager();
        propertiesGenerator = new ServerPropertiesGenerator();
        serverSettingsConfig = new ServerSettingsConfig();
        templateManager = new TemplateManager();
        serverGroupManager = new ServerGroupManager();
        serverManager = new CloudServerManager();
        playerManager = new CloudPlayerManager();
        network = new NetworkNettyServer(config.getNetworkAddress());
        httpServer = new HttpServer(config.getHttpServerAddress());
        requestManager = new RequestManager();
        packetPool = new PacketPool(this::registerPackets);
        clientCache = new ServerClientCache();
        trafficMonitorManager = new TrafficMonitorManager();
        pluginManager = new CloudPluginManager();
        eventManager = new EventManager();

        loader.registerPreAll(serverSoftwareManager, libraryManager);
        loader.registerAll(commandManager, packetPool, pluginManager, templateManager, propertiesGenerator, trafficMonitorManager);

        loader.preloadAll();

        printBanner();
        logger.info("The §bCloud §ris §astarting§r...");

        CloudProvider.select();

        for (Map<String, Object> map : startNotifications) {
            String message = map.get("message").toString();
            CloudLogLevel level = (CloudLogLevel) map.get("level");
            Object[] args = (Object[]) map.get("args");
            CloudLogger.get().log(level, message, args);
        }

        ticker.registerAll(console, requestManager, clientCache, trafficMonitorManager, pluginManager, templateManager, serverManager);

        loader.loadAll();

        if (serverSoftwareManager.getAll().isEmpty()) {
            CloudLogger.get().warn("No software found, therefore no server can be started.");
        }

        network.start();
        if (config.isHttpServerEnabled()) httpServer.start();

        Runtime.getRuntime().addShutdownHook(new CloudShutdownHook());

        BenchmarkTiming result = Benchmark.stopTiming("cloud_start");
        logger.info("§bCloud §rhas been §astarted§r. §8(§rTook §b{}s§8)", NumberUtils.formatNumber(result.duration() / 1000, 3));
        startTime = System.currentTimeMillis();

        ticker.tick();
    }

    public PocketCloud appendStartNotification(String message, CloudLogLevel level, Object... args) {
        if (currentTick() > 0) {
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
        console.clear();
        logger.emptyLine().setFormat("§r{message}")
                .info("  §bPocket§3Cloud §8- §rA cloud system for §lPocketMine-MP servers§r with §lProxy support§r §8- §b{} §8- §rdeveloped by §b{}", VersionInfo.VERSION + (VersionInfo.BETA ? "§c@BETA" : ""), String.join("§8, §b", VersionInfo.DEVELOPERS))
                .info("  Join our discord for information: §bhttps://discord.gg/3HbPEpaE3T")
                .emptyLine().resetFormat();
    }

    private void createDirectories() {
        FileUtils.createDirs(PocketCloudPaths.ALL_DIRECTORIES.stream().map(Path::of).toArray(Path[]::new));
    }

    public void reload() {
        if (!running || hasStopped) return;
        if (loader.isReloading()) return;
        logger.info("Reloading...");
        logger.warn("§cNOTE: §rNot everything is reloadable. To achieve the best outcome, restarting the cloud would be the better option.");
        try {
            boolean httpServerBeforeReload = config.isHttpServerEnabled();
            config.reload();
            serverSettingsConfig.reload();
            loader.reload();
            if (!httpServerBeforeReload && config.isHttpServerEnabled()) {
                httpServer.start();
            } else if (httpServerBeforeReload && !config.isHttpServerEnabled()) {
                httpServer.close();
            }

            logger.success("Reload complete.");
        } catch (Exception e) {
            logger.exception("Failed to reload", e);
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
            logger.exception("Unable to shutdown", e);
        }

        System.exit(0);
    }

    private void shutdown0() {
        OutputManager.reset();

        if (screenManager != null) screenManager.reset();

        logger.info("Shutting down...");

        logger.info("§cStopping §rall servers...");
        if (serverManager != null) serverManager.stopAll();

        if (loader != null) loader.unloadAll();
        if (network != null) network.close();

        logger.success("§cStopped §rthe §bcloud§r.");
        if (console != null) console.uninstall();
    }

    private void registerPackets(PacketPool pool) {
        pool.register(CommandExecuteRequestPacket.class, CommandExecuteRequestPacket::new);
        pool.register(PlayerNotificationCheckRequestPacket.class, PlayerNotificationCheckRequestPacket::new);
        pool.register(PlayerWhitelistCheckRequestPacket.class, PlayerWhitelistCheckRequestPacket::new);
        pool.register(ServerHandshakeRequestPacket.class, ServerHandshakeRequestPacket::new);
        pool.register(ServerSaveRequestPacket.class, ServerSaveRequestPacket::new);
        pool.register(ServerStartRequestPacket.class, ServerStartRequestPacket::new);
        pool.register(ServerStopRequestPacket.class, ServerStopRequestPacket::new);

        pool.register(CommandExecuteResponsePacket.class, CommandExecuteResponsePacket::new);
        pool.register(PlayerNotificationCheckResponsePacket.class, PlayerNotificationCheckResponsePacket::new);
        pool.register(PlayerWhitelistCheckResponsePacket.class, PlayerWhitelistCheckResponsePacket::new);
        pool.register(ServerHandshakeResponsePacket.class, ServerHandshakeResponsePacket::new);
        pool.register(ServerSaveResponsePacket.class, ServerSaveResponsePacket::new);
        pool.register(ServerStartResponsePacket.class, ServerStartResponsePacket::new);
        pool.register(ServerStopResponsePacket.class, ServerStopResponsePacket::new);

        pool.register(BulkSyncPacket.class, BulkSyncPacket::new);
        pool.register(CloudNotificationPacket.class, CloudNotificationPacket::new);
        pool.register(CloudSyncServerStoragePacket.class, CloudSyncServerStoragePacket::new);
        pool.register(ConsoleLogPacket.class, ConsoleLogPacket::new);
        pool.register(DisconnectPacket.class, DisconnectPacket::new);
        pool.register(KeepAlivePacket.class, KeepAlivePacket::new);
        pool.register(LanguageSyncPacket.class, LanguageSyncPacket::new);
        pool.register(LibrarySyncPacket.class, LibrarySyncPacket::new);
        pool.register(MaintenanceListSyncPacket.class, MaintenanceListSyncPacket::new);
        pool.register(ModuleSyncPacket.class, ModuleSyncPacket::new);
        pool.register(NotificationListSyncPacket.class, NotificationListSyncPacket::new);
        pool.register(PlayerConnectPacket.class, PlayerConnectPacket::new);
        pool.register(PlayerDisconnectPacket.class, PlayerDisconnectPacket::new);
        pool.register(PlayerKickPacket.class, PlayerKickPacket::new);
        pool.register(PlayerSwitchServerPacket.class, PlayerSwitchServerPacket::new);
        pool.register(PlayerSyncPacket.class, PlayerSyncPacket::new);
        pool.register(PlayerTextPacket.class, PlayerTextPacket::new);
        pool.register(PlayerTransferPacket.class, PlayerTransferPacket::new);
        pool.register(PlayerUpdateNotificationStatePacket.class, PlayerUpdateNotificationStatePacket::new);
        pool.register(ProxyRegisterServerPacket.class, ProxyRegisterServerPacket::new);
        pool.register(ProxyUnregisterServerPacket.class, ProxyUnregisterServerPacket::new);
        pool.register(ServerChangeStatusPacket.class, ServerChangeStatusPacket::new);
        pool.register(ServerGroupSyncPacket.class, ServerGroupSyncPacket::new);
        pool.register(ServerSyncPacket.class, ServerSyncPacket::new);
        pool.register(TemplateSyncPacket.class, TemplateSyncPacket::new);
    }

    public long uptime() {
        if (startTime <= 0) return 0;
        return System.currentTimeMillis() - startTime;
    }

    public long currentTick() {
        return ticker.tickCounter();
    }
}