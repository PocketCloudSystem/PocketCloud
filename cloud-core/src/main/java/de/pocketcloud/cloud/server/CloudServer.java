package de.pocketcloud.cloud.server;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cache.NotificationListCache;
import de.pocketcloud.cloud.cache.WhitelistCache;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.log.def.PrefixedLogger;
import de.pocketcloud.cloud.event.impl.server.ServerPrepareEvent;
import de.pocketcloud.cloud.event.impl.server.ServerSaveEvent;
import de.pocketcloud.cloud.event.impl.server.ServerSendCommandEvent;
import de.pocketcloud.cloud.event.impl.server.ServerStopEvent;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.server.config.IServerProperties;
import de.pocketcloud.cloud.server.util.*;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.common.config.Config;
import de.pocketcloud.common.config.exception.UnsupportedFileExtensionException;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.common.util.ProcessUtils;
import de.pocketcloud.common.util.StringUtils;
import de.pocketcloud.network.packet.impl.DisconnectPacket;
import de.pocketcloud.network.packet.impl.ProxyRegisterServerPacket;
import de.pocketcloud.network.packet.impl.ProxyUnregisterServerPacket;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.network.packet.impl.request.client.CommandExecuteRequestPacket;
import de.pocketcloud.network.packet.impl.response.client.CommandExecuteResponsePacket;
import de.pocketcloud.shared.component.BaseCloudServer;
import de.pocketcloud.shared.component.data.CloudServerData;
import de.pocketcloud.shared.component.software.ServerSoftware;
import de.pocketcloud.shared.component.storage.BaseCloudServerStorage;
import de.pocketcloud.shared.event.server.ServerChangedStatusEvent;
import de.pocketcloud.shared.event.server.ServerStartingEvent;
import de.pocketcloud.shared.network.packet.type.NotificationType;
import de.pocketcloud.shared.network.packet.type.ServerCommandExecutionResult;
import de.pocketcloud.shared.network.packet.type.ServerDisconnectReason;
import de.pocketcloud.shared.sync.SyncType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.pocketcloud.common.util.FileUtils.IO_EXECUTOR;

@Getter
@Accessors(fluent = true, chain = false)
public final class CloudServer extends BaseCloudServer implements Tickable, SyncingElement<CloudServer> {

    /**
     * Only meant for the SyncPacket
     */
    @Getter(AccessLevel.NONE)
    private transient boolean markedForRemoval = false;

    private transient final Map<String, ServerCommandExecutionRequest> commandExecutionOrders = new HashMap<>();

    private transient boolean pidLookupDone = false;
    private transient long lastPidLookupTime = 0;
    private transient int lastPidLookupCounter = 0;

    private final transient LatestPacketInfo latestPacketInfo = new LatestPacketInfo();

    private final transient PrefixedLogger logger = CloudLogger.prefixed("§8[§b" + name() + "§r§8]§r");
    @Setter
    private transient Instant lastKeepAlive = null;
    private transient Instant stopTime = null;
    private transient Config mainProperties = null;

    @MapCreator
    public CloudServer(
            @MapKey(name = "id") int id,
            @MapKey(name = "uuid") UUID uuid,
            @MapKey(name = "templateName") String templateName,
            @MapKey(name = "data") CloudServerData data,
            @MapKey(name = "storage", impl = CloudServerStorage.class) BaseCloudServerStorage storage
    ) {
        super(id, uuid, templateName, data, storage);
    }

    public CloudServer markForRemoval() {
        this.markedForRemoval = true;
        return this;
    }

    @Override
    public void syncIn(CloudServer data) {}

    @Override
    public void syncOut() {
        SyncPacket.create(SyncType.SERVER, data -> data.writeAll(this, markedForRemoval)).broadcast();
    }

    @Override
    public void tick(long currentTick) {
        if (startTime == null) return;
        if (status.isStarting()) {
            if (!pidLookupDone && data.usableProcessId() == null && (System.currentTimeMillis() - lastPidLookupTime) >= 1500 && lastPidLookupCounter < 5) {
                lastPidLookupTime = System.currentTimeMillis();
                lastPidLookupCounter++;
                ServerStartMethods.current().lookupPid(this).thenSuccess(pid -> {
                    if (pid.isPresent()) {
                        pidLookupDone = true;
                        data.processId(pid.get());
                    }
                });
            }

            if ((startTime.toEpochMilli() + (TemplateTypeHelper.timeout(template().templateType()) * 1000L)) < System.currentTimeMillis()) {
                CloudServersHandler.handleStartFailure(this, null, false);
            }
        } else if (status.isOnline()) {
            for (Map.Entry<String, ServerCommandExecutionRequest> entry : commandExecutionOrders.entrySet()) {
                ServerCommandExecutionRequest request = entry.getValue();
                if ((request.time() + 5000L) <= System.currentTimeMillis()) {
                    request.promise().reject(new TimeoutException("Request timed out"));
                    commandExecutionOrders.remove(entry.getKey());
                }
            }

            int baseTimeout = TemplateTypeHelper.timeout(template().templateType());
            double tps = PocketCloud.instance().performanceStats().currentTPS();
            double loadFactor = Math.max(1.0, 20 / Math.max(1.0, tps));
            int effectiveTimeout = (int) (baseTimeout * loadFactor);
            Instant lastKeepAliveTime = lastKeepAlive == null ? startTime : lastKeepAlive;
            if (Duration.between(lastKeepAliveTime, Instant.now()).compareTo(Duration.ofSeconds(effectiveTimeout)) > 0) {
                CloudServersHandler.handleTimeout(this);
            }
        } else if (status.isStopping()) {
            if ((stopTime.toEpochMilli() + 10_000) <= System.currentTimeMillis()) {
                CloudServersHandler.handleStopTimeout(this);
            }
        }
    }

    public Promise<Void> prepare() {
        new ServerPrepareEvent(this).call();
        return Promise.runAsync(() -> {
            Path logFileLocation = logFilePath();
            if (Files.exists(logFileLocation)) {
                Path logArchivePath = template().path().resolve("cloud_log_archive");
                FileUtils.createDir(logArchivePath);

                try {
                    FileTime ctime = Files.readAttributes(logFileLocation, BasicFileAttributes.class).creationTime();
                    String timestamp = DateTimeFormatter
                            .ofPattern("yyyy-MM-dd_HH:mm:ss.SSS_z")
                            .withZone(ZoneId.systemDefault())
                            .format(ctime.toInstant());
                    String archiveFileName = timestamp + "_" + logFileLocation.getFileName() + ".log";
                    Files.copy(logFileLocation, logArchivePath.resolve(archiveFileName), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    CloudLogger.get().exception("Failed to archive log file", e);
                }

                FileUtils.unlinkFile(logFileLocation);
            }

            if (Files.exists(path()) && !template().settings().staticServers()) {
                FileUtils.removeDirectory(path());
            }

            Path bridgePath = PocketCloud.instance().software().bridgeSourceFile((ServerSoftware) template().serverSoftware());
            Path bridgeDstPath = path().resolve(template().serverSoftware().bridge().relativeServerPath());
            if (!Files.isDirectory(bridgeDstPath.getParent())) FileUtils.createDir(bridgeDstPath.getParent());
            if (Files.isRegularFile(bridgePath)) {
                try {
                    Files.copy(bridgePath, bridgeDstPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    CloudLogger.get().exception("Failed to copy bridge", e);
                }
            }

            Path globalTemplatePath = TemplateTypeHelper.globalTemplatePath(template().templateType());
            FileUtils.copyDirectory(globalTemplatePath, path(), Set.of());

            boolean copyFromSources = template().settings().alwaysCopyToStaticServers() || !template().settings().staticServers();
            if (copyFromSources) {
                for (IServerGroup group : template().parentGroups()) {
                    FileUtils.copyDirectory(((ServerGroup) group).path(), path(), Set.of());
                }

                FileUtils.copyDirectory(template().path(), path(), Set.of("cloud_log_archive/"));
            }

            List<IServerProperties> propertiesData = PocketCloud.instance().properties().getAll(template().serverSoftware());
            for (var properties : propertiesData) {
                Path filePath = path().resolve(properties.getFileName());
                if (!copyFromSources) {
                    try {
                        Files.copy(
                                globalTemplatePath.resolve(properties.getFileName()),
                                filePath,
                                StandardCopyOption.REPLACE_EXISTING
                        );
                    } catch (IOException e) {
                        CloudLogger.get().exception("Failed to copy properties", e);
                    }
                }

                Map<String, Object> replacements = properties.replacePlaceholders(this);
                String content = FileUtils.fileGetContents(filePath);

                for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                    Object value = entry.getValue();
                    String key = entry.getKey();

                    if (value instanceof Number || value instanceof Boolean) {
                        content = content.replace("\"" + key + "\"", value.toString());
                        content = content.replace("'" + key + "'", value.toString());
                    }

                    content = content.replace(key, value.toString());
                }

                FileUtils.filePutContents(filePath, content);
            }

            FileUtils.unlinkFile(logFileLocation);
        }, IO_EXECUTOR);
    }

    public CloudServer start() {
        startTime = Instant.now();
        CloudAPI.instance().events().call(new ServerStartingEvent(this));
        status(ServerStatus.STARTING);
        CloudLogger.get().info("§aStarting §b{} §8[§ruuid={}, path={}, port={}§8]§r...", name(), uuid.toString(), path().toString(), data.port());
        PocketCloud.instance().notifications().sendNotification(NotificationType.SERVER_STARTING, Map.of("server", name()), Map.of());
        return this;
    }

    public void boot() {
        ServerStartMethods.current().start(new CloudServer[]{this})
                .thenSuccess(m -> CloudServersHandler.handleStartSuccess(this, m.get(name())))
                .failure(t -> CloudServersHandler.handleStartFailure(this, t, true));
    }

    public void stop(boolean force) {
        new ServerStopEvent(this, force).call();
        PocketCloud.instance().notifications().sendNotification(NotificationType.SERVER_STOPPING, Map.of("server", name()), Map.of());
        stopTime = Instant.now();

        if (force) {
            CloudLogger.get().info("§cStopped §b{} §rforcefully.", name());
            status(ServerStatus.OFFLINE);
            kill();
            remove();
            deleteTmpDir();
        } else {
            CloudLogger.get().info("§cStopping §b{}§r...", name());
            status(ServerStatus.STOPPING);
            sendPacket(DisconnectPacket.create(ServerDisconnectReason.SERVER_SHUTDOWN));
        }
    }

    public void stop() {
        stop(false);
    }

    public void saveAndDeleteLogFiles() {
        Path logFileLocation = logFilePath();
        Path logArchivePath = template().path().resolve("cloud_log_archive");
        if (Files.exists(logFileLocation)) {
            if (!Files.isDirectory(logArchivePath)) {
                try {
                    Files.createDirectory(logArchivePath);
                } catch (IOException e) {
                    CloudLogger.get().exception("Failed to create log archive directory", e);
                }
            }

            String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS_z")
                    .format(startTime.atZone(ZoneId.systemDefault()));

            try {
                Files.copy(logFileLocation, logArchivePath.resolve(formatted + "_" + logFileLocation.getFileName().toString() + ".log"));
            } catch (IOException e) {
                CloudLogger.get().exception("Failed to copy log file", e);
            }
        }
    }

    public Promise<Void> save() {
        if (!new ServerSaveEvent(this).call().isCancelled()) return Promise.rejected(new RuntimeException("Event cancelled"));
        return Promise.supplyAsync(() -> {
            for (String file : template().serverSoftware().config().savableFiles()) {
                Path filePath = path().resolve(file);
                Path dstPath = template().path().resolve(file);
                if (Files.isRegularFile(filePath)) {
                    try {
                        Files.copy(filePath, dstPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (Files.isDirectory(filePath)) {
                    FileUtils.copyDirectory(filePath, dstPath, Set.of());
                }
            }

            return null;
        }, IO_EXECUTOR);
    }

    public void deleteTmpDir() {
        saveAndDeleteLogFiles();
        if (!template().settings().staticServers()) {
            CloudLogger.get().debug("Removed server directory {}", path().toString());
            FileUtils.removeDirectory(path());
        }
    }

    public void remove() {
        PocketCloud.instance().servers().remove(this);
        PocketCloud.instance().clients().remove(this);

        if (template().templateType() == TemplateType.SERVER) {
            if (template().settings().saveOnShutdown()) save();
            removeFromProxies();
        }
    }

    public void kill() {
        Long pid = data.usableProcessId();
        if (pid != null) ProcessUtils.kill(pid, true);
    }

    public Promise<ServerCommandExecutionResult> dispatch(String commandLine) {
        Optional<ServerClient> client = client();
        if (client.isEmpty()) return Promise.rejected(new IllegalStateException("Not verified yet"));
        if (new ServerSendCommandEvent(this, commandLine).call().isCancelled()) return Promise.rejected(new RuntimeException("Event cancelled"));
        String id = "command-" + StringUtils.generate(10);
        Promise<ServerCommandExecutionResult> promise = new Promise<>();
        CommandExecuteRequestPacket.create(commandLine, id).sendRequest(client.get()).then(response -> {
            if (response instanceof CommandExecuteResponsePacket p) {
                promise.resolve(p.getCommandExecutionResult());
            } else {
                promise.reject(new IllegalStateException("Received unexpected response: " + response.getClass().getName()));
            }

            commandExecutionOrders.remove(id);
        }).failure((_, e, reason) -> promise.reject(Objects.requireNonNullElseGet(e, () -> new RuntimeException("RequestActionFailureReason: " + reason.name()))));

        commandExecutionOrders.put(id, new ServerCommandExecutionRequest(id, promise, System.currentTimeMillis()));

        return promise;
    }

    public void addToProxies() {
        if (template().templateType() == TemplateType.SERVER) {
            ProxyRegisterServerPacket.create(this).broadcast(e -> e.templateType(TemplateType.SERVER));
        }
    }

    public void removeFromProxies() {
        if (template().templateType() == TemplateType.SERVER) {
            ProxyUnregisterServerPacket.create(this).broadcast(e -> e.templateType(TemplateType.SERVER));
        }
    }

    public void sync() {
        List<ClientboundPacket> syncPackets = new ArrayList<>(List.of(
                PocketCloud.instance().libraries().buildSyncPacket(this),
                LocalCache.get(WhitelistCache.class).buildSyncPacket(),
                LocalCache.get(NotificationListCache.class).buildSyncPacket(),
                SyncPacket.create(SyncType.LANGUAGE, pData -> {
                    pData.write(PocketCloud.instance().language().current().id());
                    pData.write(PocketCloud.instance().language().current().messages());
                }),
                SyncPacket.create(SyncType.SERVERS, pData -> pData.write(PocketCloud.instance().servers().getAll().stream().map(ICloudServer::write).toList())),
                SyncPacket.create(SyncType.TEMPLATES, pData -> pData.write(PocketCloud.instance().templates().getAll().stream().map(ITemplate::write).toList())),
                SyncPacket.create(SyncType.PLAYERS, pData -> pData.write(PocketCloud.instance().players().getAll().stream().map(ICloudPlayer::write).toList())),
                SyncPacket.create(SyncType.SERVER_GROUPS, pData -> pData.write(PocketCloud.instance().serverGroups().getAll().stream().map(IServerGroup::write).toList())),
                SyncPacket.create(SyncType.SOFTWARES, pData -> pData.write(PocketCloud.instance().softwares().getAll().stream().map(IServerSoftware::write).toList()))
        ));

        if (template().templateType().isProxy()) {
            for (ICloudServer subServer : PocketCloud.instance().servers().query(ServerSearchQuery.create().ofType(TemplateType.SERVER))) {
                if (subServer.status().isOnline()) {
                    syncPackets.add(ProxyRegisterServerPacket.create(subServer));
                }
            }
        }

        for (ClientboundPacket packet : syncPackets) {
            sendPacket(packet);
        }
    }

    public CompletableFuture<Void> sendPacket(ClientboundPacket packet) {
        if (client().isEmpty()) return CompletableFuture.failedFuture(new RuntimeException("Client is empty"));
        return client().get().sendPacket(packet);
    }

    public CompletableFuture<Void> sendDelayedPacket(ClientboundPacket packet, long delay, TimeUnit unit) {
        if (client().isEmpty()) return CompletableFuture.failedFuture(new RuntimeException("Client is empty"));
        return client().get().sendDelayedPacket(packet, delay, unit);
    }

    public CompletableFuture<Void> sendDelayedPacket(ClientboundPacket packet, long ticks) {
        if (client().isEmpty()) return CompletableFuture.failedFuture(new RuntimeException("Client is empty"));
        return client().get().sendDelayedPacket(packet, ticks);
    }

    @Override
    public void status(ServerStatus status) {
        ServerStatus oldStatus = this.status;
        this.status = status;
        syncOut();
        CloudAPI.instance().events().call(new ServerChangedStatusEvent(this, oldStatus, status));
    }

    public ServerLogStream openLogStream() {
        return new ServerLogStream(this);
    }

    public boolean isAlive() {
        if (data.usableProcessId() == null) return false;
        Optional<ProcessHandle> proc = ProcessHandle.of(data.usableProcessId());
        return proc.isPresent() && proc.get().isAlive();
    }

    @Override
    public CloudServerStorage storage() {
        return (CloudServerStorage) super.storage;
    }

    @Override
    public Template template() {
        return (Template) super.template();
    }

    public Path path() {
        if (template().settings().staticServers()) return PocketCloudPaths.storage().staticServers().with(name()).asPath();
        return PocketCloudPaths.tmp().with(name() + "_" + uuid.toString()).asPath();
    }

    public Path logFilePath() {
        return path().resolve(template().serverSoftware().config().relativeLogFileLocation());
    }

    public Path customLogFilePath() {
        return path().resolve(".cloud.console.log");
    }

    public Config properties() {
        if (mainProperties == null) {
            try {
                mainProperties = new Config(path().resolve(template().serverSoftware().config().mainConfigurationFile()));
            } catch (IOException | UnsupportedFileExtensionException e) {
                throw new RuntimeException(e);
            }
        }

        return mainProperties;
    }

    public Optional<ServerClient> client() {
        return PocketCloud.instance().clients().get(this);
    }

    public static CloudServer read(Map<String, Object> data) {
        return MapperUtils.fromMap(data, CloudServer.class);
    }
}