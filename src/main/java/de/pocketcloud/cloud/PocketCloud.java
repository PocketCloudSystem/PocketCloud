package de.pocketcloud.cloud;

import de.pocketcloud.cloud.config.impl.MainConfig;
import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.CloudShutdownHook;
import de.pocketcloud.cloud.console.command.CommandManager;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.log.def.MainLogger;
import de.pocketcloud.cloud.event.EventManager;
import de.pocketcloud.cloud.load.Loader;
import de.pocketcloud.cloud.network.NettyServer;
import de.pocketcloud.cloud.network.client.ServerClientCache;
import de.pocketcloud.cloud.network.packet.PacketPool;
import de.pocketcloud.cloud.network.request.RequestManager;
import de.pocketcloud.cloud.plugin.CloudPluginManager;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.server.library.LibraryManager;
import de.pocketcloud.cloud.server.software.ServerSoftwareManager;
import de.pocketcloud.cloud.template.TemplateManager;
import de.pocketcloud.cloud.tick.Ticker;
import de.pocketcloud.cloud.traffic.TrafficMonitorManager;
import de.pocketcloud.cloud.util.FileUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.cloud.util.Utils;
import de.pocketcloud.cloud.util.VersionInfo;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.cloud.util.benchmark.BenchmarkTiming;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

@Getter
@Accessors(fluent = true)
public final class PocketCloud {

    public static void main(String[] args) throws IOException {
        new PocketCloud();
    }

    @Getter
    @Accessors(fluent = false)
    private static PocketCloud instance = null;

    private boolean running;
    private boolean hasStopped = false;

    private Ticker ticker = null;
    private Loader loader = null;

    private MainConfig config = null;
    private MainLogger logger = null;
    private CloudConsole console = null;
    private CommandManager commandManager = null;
    private ServerSoftwareManager serverSoftwareManager = null;
    private LibraryManager libraryManager = null;
    private TemplateManager templateManager = null;
    private NettyServer network = null;
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

        Thread.setDefaultUncaughtExceptionHandler((_, throwable) -> CloudLogger.get().exception(throwable));

        console = new CloudConsole();
        console.install();
        console.start();

        commandManager = new CommandManager();
        serverSoftwareManager = new ServerSoftwareManager();
        libraryManager = new LibraryManager();
        templateManager = new TemplateManager();
        network = new NettyServer(new InetSocketAddress(config.network().get("address").toString(), Integer.parseInt(config.network().get("port").toString())));
        requestManager = new RequestManager();
        packetPool = new PacketPool();
        clientCache = new ServerClientCache();
        trafficMonitorManager = new TrafficMonitorManager();
        pluginManager = new CloudPluginManager();
        eventManager = new EventManager();

        loader.registerPreAll(serverSoftwareManager, libraryManager);
        loader.registerAll(commandManager, packetPool, pluginManager, templateManager);

        loader.preloadAll();

        printBanner();
        logger.info("The §bCloud §ris §astarting§r...");

        CloudProvider.select();

        ticker.registerAll(console, requestManager, clientCache, trafficMonitorManager, pluginManager);

        loader.loadAll();

        if (serverSoftwareManager.getAll().isEmpty()) {
            CloudLogger.get().warn("No software found, therefore no server can be started.");
        }

        network.start();

        Runtime.getRuntime().addShutdownHook(new CloudShutdownHook());

        BenchmarkTiming result = Benchmark.stopTiming("cloud_start");
        logger.info("§bCloud §rhas been §astarted§r. §8(§rTook §b{}s§8)", Utils.formatNumber(result.duration() / 1000, 3));

        ticker.tick();
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
            config.reload();
            loader.reload();
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
        logger.info("Shutting down...");

        if (loader != null) loader.unloadAll();
        if (network != null) network.close();

        logger.success("§cStopped §rthe §bcloud§r.");
        if (console != null) console.uninstall();
    }
}