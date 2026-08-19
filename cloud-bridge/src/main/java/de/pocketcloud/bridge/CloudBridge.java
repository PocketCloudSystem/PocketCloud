package de.pocketcloud.bridge;

import de.pocketcloud.api.CloudAPI;
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

        registry.register(TrafficMonitorManager.class, new TrafficMonitorManager());
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

        try {
            network().start();
        } catch (InterruptedException e) {
            logger().error("Failed to establish network connection, shutting this server down...", e);
            shutdown();
            return;
        }

        platformPlugin.startTask(new RequestTimeoutTask(), 10);

        ServerHandshakeRequestPacket.create(
                environmentConfig().localServerName(),
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
            logger.warn("Cloud did not respond to ServerHandshakeRequestPacket, shutting down...");
            if (t != null) logger.exception(t);
            shutdown();
        });
    }

    private void startOtherTasks() {
        platformPlugin().startTask(new ChangeStatusTask(), 10);
        platformPlugin().startTask(new ServerTimeoutTask(), 10);
        platformPlugin().startTask(new UpdatePerformanceStatsTask(), 10);
    }

    public void shutdown() {
        logger().warn("Plugin has been disabled, shutting down...");
        DisconnectPacket.create(ServerDisconnectReason.SERVER_SHUTDOWN).sendPacket();
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