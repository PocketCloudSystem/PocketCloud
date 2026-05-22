package de.pocketcloud.cloud;

import de.pocketcloud.cloud.config.impl.MainConfig;
import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.CloudShutdownHook;
import de.pocketcloud.cloud.console.command.CommandManager;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.log.def.MainLogger;
import de.pocketcloud.cloud.event.EventManager;
import de.pocketcloud.cloud.network.NettyServer;
import de.pocketcloud.cloud.network.client.ServerClientCache;
import de.pocketcloud.cloud.network.packet.PacketPool;
import de.pocketcloud.cloud.network.request.RequestManager;
import de.pocketcloud.cloud.plugin.CloudPluginManager;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.server.software.ServerSoftwareManager;
import de.pocketcloud.cloud.tick.Ticker;
import de.pocketcloud.cloud.traffic.TrafficMonitorManager;
import de.pocketcloud.cloud.util.FileUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.cloud.util.VersionInfo;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.cloud.util.benchmark.BenchmarkTiming;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;

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

    private MainConfig config = null;
    private MainLogger logger = null;
    private CloudConsole console = null;
    private CommandManager commandManager = null;
    private ServerSoftwareManager serverSoftwareManager = null;
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

        //TODO download libs, binnaries

        serverSoftwareManager.load();

        printBanner();
        logger.info("The §bCloud §ris §astarting§r...");

        CloudProvider.select();

        network = new NettyServer(new InetSocketAddress(config.network().get("address").toString(), Integer.parseInt(config.network().get("port").toString())));
        requestManager = new RequestManager();
        packetPool = new PacketPool();
        clientCache = new ServerClientCache();
        trafficMonitorManager = new TrafficMonitorManager();
        pluginManager = new CloudPluginManager();
        eventManager = new EventManager();

        ticker.registerAll(console, requestManager, clientCache, trafficMonitorManager, pluginManager);

        if (serverSoftwareManager.getAll().isEmpty()) {
            CloudLogger.get().warn("No software found, therefore no server can be started.");
        }

        try {
            network.start();
        } catch (InterruptedException e) {
            logger.exception("Unable to start netty server", e);
            shutdown();
        }

        Runtime.getRuntime().addShutdownHook(new CloudShutdownHook());

        BenchmarkTiming result = Benchmark.stopTiming("cloud_start");
        BigDecimal bd = BigDecimal.valueOf(result.duration() / 1000).setScale(3, RoundingMode.HALF_UP);
        logger.info("§bCloud §rhas been §astarted§r. §8(§rTook §b{}s§8)", bd.floatValue());

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
        FileUtils.createDirs(PocketCloudPaths.ALL_DIRECTORIES.toArray(new String[0]));
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

        pluginManager.disableAll();
        network.close();

        logger.success("§cStopped §rthe §bcloud§r.");
        console.uninstall();
    }
}