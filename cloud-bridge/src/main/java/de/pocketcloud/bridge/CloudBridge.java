package de.pocketcloud.bridge;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.cluster.INodeProvider;
import de.pocketcloud.api.config.IEnvironmentConfig;
import de.pocketcloud.api.event.EventService;
import de.pocketcloud.api.executor.IPlayerExecutor;
import de.pocketcloud.api.language.LanguageKey;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.api.service.ServiceRegistry;
import de.pocketcloud.bridge.adapter.NativePlayerAdapter;
import de.pocketcloud.bridge.api.IPlatformPlugin;
import de.pocketcloud.bridge.cache.RandomCache;
import de.pocketcloud.bridge.executor.BridgePlayerExecutor;
import de.pocketcloud.bridge.network.NetworkNettyClient;
import de.pocketcloud.bridge.network.packet.PacketRegistry;
import de.pocketcloud.bridge.notification.NotificationService;
import de.pocketcloud.bridge.provider.*;
import de.pocketcloud.bridge.task.ChangeStatusTask;
import de.pocketcloud.bridge.task.RequestTimeoutTask;
import de.pocketcloud.bridge.task.ServerTimeoutTask;
import de.pocketcloud.bridge.task.UpdatePerformanceStatsTask;
import de.pocketcloud.bridge.util.ProcessPerformanceStats;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.network.packet.impl.DisconnectPacket;
import de.pocketcloud.network.packet.impl.KeepAlivePacket;
import de.pocketcloud.network.packet.impl.request.ServerHandshakeRequestPacket;
import de.pocketcloud.network.packet.impl.response.ServerHandshakeResponsePacket;
import de.pocketcloud.network.request.RequestManager;
import de.pocketcloud.network.traffic.TrafficMonitorManager;
import de.pocketcloud.shared.event.server.LocalServerReadyEvent;
import de.pocketcloud.shared.network.packet.type.ServerDisconnectReason;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class CloudBridge implements CloudAPI {

    @Getter
    private static CloudBridge instance;

    private VerificationStatus status = VerificationStatus.PENDING;
    private final ServiceRegistry registry = new ServiceRegistry();
    private volatile boolean shutdown = false;
    private volatile boolean reconnecting = false;
    private volatile long reconnectDeadline = 0;
    private final java.util.concurrent.ScheduledExecutorService reconnectExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "PocketCloud-Reconnect");
        t.setDaemon(true);
        return t;
    });

    public CloudBridge(IPlatformPlugin platformPlugin, ILogger logger, IEnvironmentConfig config, NativePlayerAdapter<?> nativePlayerAdapter) {
        instance = this;

        registry.register(IPlatformPlugin.class, platformPlugin);
        registry.register(ILogger.class, logger);
        registry.register(IEnvironmentConfig.class, config);
        registry.register(ProcessPerformanceStats.class, new ProcessPerformanceStats())
                .updateStats();

        registry.register(NativePlayerAdapter.class, nativePlayerAdapter);
        registry.register(BridgePlayerExecutor.class, new BridgePlayerExecutor());
        registry.register(NotificationService.class, new NotificationService());

        registry.register(TrafficMonitorManager.class, new TrafficMonitorManager()).load();
        registry.register(NetworkNettyClient.class, new NetworkNettyClient(config.cloudNetworkAddress()));
        registry.register(RequestManager.class, new RequestManager());
        registry.register(PacketRegistry.class, new PacketRegistry());
        registry.register(EventService.class, new EventService<>());

        packets().preload();
        packets().load();

        registry.register(ServerProvider.class, new ServerProvider());
        registry.register(TemplateProvider.class, new TemplateProvider());
        registry.register(ServerGroupProvider.class, new ServerGroupProvider());
        registry.register(PlayerProvider.class, new PlayerProvider());
        registry.register(SoftwareProvider.class, new SoftwareProvider());
        registry.register(LanguageProvider.class, new LanguageProvider());
        registry.register(INodeProvider.class, new NodeProvider());

        try {
            network().start();
        } catch (Throwable e) {
            logger().error("Failed to establish network connection, shutting down...", e);
            shutdown();
            return;
        }

        platformPlugin.startTask(new RequestTimeoutTask(), 0, 10);

        ServerHandshakeRequestPacket.create(
                environmentConfig().localServerName(),
                environmentConfig().localServerUuid(),
                ProcessHandle.current().pid(),
                platformPlugin.maxPlayers()
        ).sendRequest().then(res -> {
            status = res.getVerificationStatus();
            if (res.getVerificationStatus() == VerificationStatus.VERIFIED) {
                LocalCache.get(RandomCache.class).add(RandomCache.KEY_LAST_KEEP_ALIVE, System.currentTimeMillis());
                startOtherTasks();
                platformPlugin.onVerification();
                logger.info(LanguageKey.INGAME_SERVER_VERIFIED.translate());
                constructKeepAlive().sendPacket();
                CloudAPI.instance().events().call(new LocalServerReadyEvent(CloudAPI.instance().servers().current()));
            } else {
                logger.warn("Cloud responded with verification status '{}', shutting down...", res.getVerificationStatus().name());
                shutdown();
            }
        }, ServerHandshakeResponsePacket.class).failure((req, t, r) -> {
            logger.warn("Cloud did not respond to ServerHandshakeRequestPacket: " + r.name() + ", shutting down...");
            if (t != null) logger.exception(t);
            shutdown();
        });
    }

    private void startOtherTasks() {
        platformPlugin().startTask(new ChangeStatusTask(), 0, 10);
        platformPlugin().startTask(new ServerTimeoutTask(), 0, 10);
        platformPlugin().startTask(new UpdatePerformanceStatsTask(), 0, 10);
    }

    /**
     * The connection to the cloud node is gone (e.g. the node restarts). Instead of shutting down at once,
     * try to reconnect every 2 seconds and verify again as the same server. Shuts down when the reconnect
     * timeout ({@code network-reconnect-timeout}, default 60 s) passes.
     */
    public synchronized void connectionLost() {
        if (shutdown || reconnecting) return;
        if (status != VerificationStatus.VERIFIED) {
            logger().warn("Connection to the cloud was lost before the server was verified, shutting down...");
            shutdownOnMainThread();
            return;
        }
        reconnecting = true;
        int timeout = environmentConfig() instanceof de.pocketcloud.bridge.config.LocalServerConfig c ? c.reconnectTimeout() : 60;
        reconnectDeadline = System.currentTimeMillis() + timeout * 1000L;
        logger().warn("Connection to the cloud was lost, trying to reconnect for " + timeout + " seconds...");
        reconnectExecutor.schedule(this::reconnectAttempt, 2, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void reconnectAttempt() {
        if (shutdown) return;
        if (System.currentTimeMillis() > reconnectDeadline) {
            logger().warn("Could not reconnect to the cloud in time, shutting down...");
            reconnecting = false;
            shutdownOnMainThread();
            return;
        }

        try {
            network().disconnect();
            network().start();
        } catch (Throwable t) {
            scheduleNextAttempt();
            return;
        }

        ServerHandshakeRequestPacket.create(
                environmentConfig().localServerName(),
                environmentConfig().localServerUuid(),
                ProcessHandle.current().pid(),
                platformPlugin().maxPlayers()
        ).sendRequest(5000).then(res -> {
            if (res.getVerificationStatus() == VerificationStatus.VERIFIED) {
                reconnecting = false;
                LocalCache.get(RandomCache.class).add(RandomCache.KEY_LAST_KEEP_ALIVE, System.currentTimeMillis());
                logger().info("Reconnected to the cloud and verified again.");
                constructKeepAlive().sendPacket();
            } else {
                logger().info("Cloud answered the reconnect with " + res.getVerificationStatus().name() + ", retrying...");
                scheduleNextAttempt();
            }
        }, ServerHandshakeResponsePacket.class).failure((req, t, r) -> scheduleNextAttempt());
    }

    private void scheduleNextAttempt() {
        if (shutdown) return;
        reconnectExecutor.schedule(this::reconnectAttempt, 2, java.util.concurrent.TimeUnit.SECONDS);
    }

    public boolean stopping() {
        return shutdown;
    }

    private void shutdownOnMainThread() {
        platformPlugin().startTask(this::shutdown, 1, 0);
    }

    public void shutdown() {
        if (shutdown) return;
        shutdown = true;
        reconnectExecutor.shutdownNow();
        logger().warn("Plugin has been disabled, shutting down...");
        if (network().connected()) DisconnectPacket.create(ServerDisconnectReason.SERVER_SHUTDOWN).sendPacket();
        network().close();

        platformPlugin().shutdownServer();
    }

    public KeepAlivePacket constructKeepAlive() {
        return KeepAlivePacket.create(
                platformPlugin().tps(),
                platformPlugin().avgTps(),
                performanceStats().usedMemory(),
                performanceStats().peakUsedMemory(),
                performanceStats().maxMemory(),
                performanceStats().cpuUsage()
        );
    }

    public IPlatformPlugin platformPlugin() {
        return registry.get(IPlatformPlugin.class);
    }

    public NotificationService notifications() {
        return registry.get(NotificationService.class);
    }

    public ProcessPerformanceStats performanceStats() {
        return registry.get(ProcessPerformanceStats.class);
    }

    public TrafficMonitorManager traffic() {
        return registry.get(TrafficMonitorManager.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventService<Object> events() {
        return (EventService<Object>) registry.get(EventService.class);
    }

    @Override
    public IPlayerExecutor playerExecutor() {
        return registry.get(BridgePlayerExecutor.class);
    }

    public NativePlayerAdapter<?> playerAdapter() {
        return registry.get(NativePlayerAdapter.class);
    }

    @Override
    public PacketRegistry packets() {
        return registry.get(PacketRegistry.class);
    }

    @Override
    public PlayerProvider players() {
        return registry.get(PlayerProvider.class);
    }

    @Override
    public ServerGroupProvider serverGroups() {
        return registry.get(ServerGroupProvider.class);
    }

    @Override
    public ServerProvider servers() {
        return registry.get(ServerProvider.class);
    }

    @Override
    public TemplateProvider templates() {
        return registry.get(TemplateProvider.class);
    }

    @Override
    public SoftwareProvider softwares() {
        return registry.get(SoftwareProvider.class);
    }

    @Override
    public LanguageProvider language() {
        return registry.get(LanguageProvider.class);
    }

    @Override
    public ILogger logger() {
        return registry.get(ILogger.class);
    }

    @Override
    public IEnvironmentConfig environmentConfig() {
        return registry.get(IEnvironmentConfig.class);
    }

    public NetworkNettyClient network() {
        return registry.get(NetworkNettyClient.class);
    }

    public RequestManager requests() {
        return registry.get(RequestManager.class);
    }

    @Override
    public ServiceRegistry services() {
        return registry;
    }
}