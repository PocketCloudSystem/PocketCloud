package de.pocketcloud.cloud.server;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.notification.Notifier;
import de.pocketcloud.common.config.Config;
import de.pocketcloud.common.config.exception.UnsupportedFileExtensionException;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.log.def.PrefixedLogger;
import de.pocketcloud.cloud.event.impl.server.ServerPrepareEvent;
import de.pocketcloud.cloud.event.impl.server.ServerStartEvent;
import de.pocketcloud.cloud.event.impl.server.ServerStopEvent;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.client.ServerClientCache;
import de.pocketcloud.common.serialization.Writable;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.common.util.ProcessUtils;
import de.pocketcloud.common.util.StringUtils;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.impl.*;
import de.pocketcloud.cloud.network.packet.impl.request.client.CommandExecuteRequestPacket;
import de.pocketcloud.cloud.network.packet.impl.response.client.CommandExecuteResponsePacket;
import de.pocketcloud.network.packet.type.NotificationType;
import de.pocketcloud.network.packet.type.ServerCommandExecutionResult;
import de.pocketcloud.network.packet.type.ServerDisconnectReason;
import de.pocketcloud.network.packet.type.VerificationStatus;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.player.CloudPlayerManager;
import de.pocketcloud.cloud.server.config.IServerProperties;
import de.pocketcloud.cloud.server.config.ServerPropertiesGenerator;
import de.pocketcloud.cloud.server.util.*;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.TemplateManager;
import de.pocketcloud.cloud.template.TemplateType;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.cloud.util.*;
import de.pocketcloud.cloud.util.concurrent.Promise;
import de.pocketcloud.common.mapper.MapperUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static de.pocketcloud.common.util.FileUtils.IO_EXECUTOR;

@Getter
@Accessors(fluent = true)
public final class CloudServer implements Tickable, Writable<Map<String, Object>>, FilterableObject {

    private final int id;
    private final UUID uuid;
    private final String templateName;
    private final CloudServerData serverData;
    private final CloudServerStorage storage;

    private transient final Map<String, ServerCommandExecutionRequest> commandExecutionOrders = new HashMap<>();

    private transient boolean pidLookupDone = false;
    private transient long lastPidLookupTime = 0;
    private transient int lastPidLookupCounter = 0;

    private transient final LatestPacketInfo latestPacketInfo = new LatestPacketInfo();

    private transient final PrefixedLogger logger;
    @Setter
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;
    private ServerStatus status = ServerStatus.PENDING;
    @Setter
    private transient Long lastKeepAlive = null;
    private Long startTime = null;
    @Setter
    private Long verifiedTime = null;
    private transient Long stopTime = null;
    private transient Config mainProperties = null;

    public CloudServer(int id, UUID uuid, String templateName, CloudServerData serverData) {
        this.id = id;
        this.uuid = uuid;
        this.templateName = templateName;
        this.serverData = serverData;
        this.storage = new CloudServerStorage(uuid);
        this.logger = new PrefixedLogger(PocketCloud.instance().logger(), "§8[§b" + name() + "§r§8]§r");
    }

    public CloudServer(int id, UUID uuid, String templateName, CloudServerData serverData, ServerStatus status) {
        this(id, uuid, templateName, serverData);
        this.status = status;
    }

    public CloudServer(int id, UUID uuid, String templateName, CloudServerData serverData, Map<String, Object> storage) {
        this(id, uuid, templateName, serverData);
        this.storage.setAll(storage);
    }

    @Override
    public void tick(long currentTick) {
        if (startTime == null) return;
        if (status.isStarting()) {
            if (!pidLookupDone && serverData.processId() == null && (System.currentTimeMillis() - lastPidLookupTime) >= 1500 && lastPidLookupCounter < 5) {
                lastPidLookupTime = System.currentTimeMillis();
                lastPidLookupCounter++;
                ServerStartMethods.current().lookupPid(this).thenSuccess(pid -> {
                    if (pid.isPresent()) {
                        pidLookupDone = true;
                        serverData.processId(pid.get());
                    }
                });
            }

            if ((startTime + (template().templateType().timeout() * 1000L)) < System.currentTimeMillis()) {
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

            if (Files.exists(path()) && !template().settings().isStaticServers()) {
                FileUtils.removeDirectory(path());
            }

            Path bridgePath = template().serverSoftware().bridge().sourceFile();
            Path bridgeDstPath = path().resolve(template().serverSoftware().bridge().relativeServerPath());
            if (!Files.isDirectory(bridgeDstPath.getParent())) FileUtils.createDir(bridgeDstPath.getParent());
            if (Files.isRegularFile(bridgePath)) {
                try {
                    Files.copy(bridgePath, bridgeDstPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    CloudLogger.get().exception("Failed to copy bridge", e);
                }
            }

            Path globalTemplatePath = template().templateType().globalTemplatePath();
            FileUtils.copyDirectory(globalTemplatePath, path(), Set.of());

            boolean copyFromSources = template().settings().isAlwaysCopyToStaticServers() || !template().settings().isStaticServers();
            if (copyFromSources) {
                for (ServerGroup group : template().parentGroups()) {
                    FileUtils.copyDirectory(group.path(), path(), Set.of());
                }

                FileUtils.copyDirectory(template().path(), path(), Set.of("cloud_log_archive/"));
            }

            List<IServerProperties> propertiesData = ServerPropertiesGenerator.instance().getAll(template().serverSoftware());
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

    @SuppressWarnings("unchecked")
    private void replacePlaceholdersRecursively(Map<String, Object> map, Map<String, Object> replacements) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String str) {
                for (Map.Entry<String, Object> repl : replacements.entrySet()) {
                    if (str.equals(repl.getKey())) {
                        entry.setValue(repl.getValue());
                        break;
                    } else if (str.contains(repl.getKey())) {
                        str = str.replace(repl.getKey(), repl.getValue().toString());
                        entry.setValue(str);
                    }
                }
            } else if (value instanceof Map) {
                replacePlaceholdersRecursively((Map<String, Object>) value, replacements);
            }
        }
    }

    public CloudServer start() {
        startTime = System.currentTimeMillis();
        setStatus(ServerStatus.STARTING);
        new ServerStartEvent(this).call();
        CloudLogger.get().info("§aStarting §b{} §8[§ruuid={}, path={}, port={}§8]§r...", name(), uuid.toString(), path().toString(), serverData.port());
        Notifier.notify(NotificationType.SERVER_STARTING, Map.of("server", name()), Map.of());
        return this;
    }

    public void boot() {
        ServerStartMethods.current().start(new CloudServer[]{this})
                .thenSuccess(m -> CloudServersHandler.handleStartSuccess(this, m.getOrDefault(name(), null)))
                .failure(t -> CloudServersHandler.handleStartFailure(this, t, true));
    }

    public void stop(boolean force) {
        new ServerStopEvent(this, force).call();
        Notifier.notify(NotificationType.SERVER_STOPPING, Map.of("server", name()), Map.of());
        stopTime = System.currentTimeMillis();

        if (force) {
            CloudLogger.get().info("§cStopped §b{} §rforcefully.", name());
            setStatus(ServerStatus.OFFLINE);
            kill();
            remove();
            //todo checkforCrash
            deleteTmpDir();
        } else {
            CloudLogger.get().info("§cStopping §b{}§r...", name());
            setStatus(ServerStatus.STOPPING);
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
                    .format(Instant.ofEpochMilli((long) Math.floor(startTime))
                            .atZone(ZoneId.systemDefault()));

            try {
                Files.copy(logFileLocation, logArchivePath.resolve(formatted + "_" + logFileLocation.getFileName().toString() + ".log"));
            } catch (IOException e) {
                CloudLogger.get().exception("Failed to copy log file", e);
            }
        }
    }

    public Promise<Void> save() {
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
        if (!template().settings().isStaticServers()) {
            CloudLogger.get().debug("Removed server directory {}", path().toString());
            FileUtils.removeDirectory(path());
        }
    }

    public void remove() {
        CloudServerManager.instance().remove(this);
        ServerClientCache.instance().remove(this);

        if (template().templateType() == TemplateType.SERVER) {
            if (template().settings().isSaveOnShutdown()) save();
            removeFromProxies();
        }
    }

    public void kill() {
        if (serverData.processId() != null) ProcessUtils.kill(serverData.processId(), true);
    }

    public Promise<ServerCommandExecutionResult> dispatch(String commandLine) {
        Optional<ServerClient> client = client();
        if (client.isEmpty()) return Promise.rejected(new IllegalStateException("Not verified yet"));
        String id = "command-" + StringUtils.generate(10);
        Promise<ServerCommandExecutionResult> promise = new Promise<>();
        CommandExecuteRequestPacket requestPacket = (CommandExecuteRequestPacket) CommandExecuteRequestPacket.create(commandLine, id).sendRequest(client.get()).then(response -> {
            if (response instanceof CommandExecuteResponsePacket p) {
                promise.resolve(p.getCommandExecutionResult());
            } else {
                promise.reject(new IllegalStateException("Received unexpected response: " + response.getClass().getName()));
            }

            commandExecutionOrders.remove(id);
        }).failure((_, e, reason) -> promise.reject(Objects.requireNonNullElseGet(e, () -> new RuntimeException("RequestActionFailureReason: " + reason.name()))));

        commandExecutionOrders.put(id, new ServerCommandExecutionRequest(id, promise, requestPacket.getSentTimestamp()));

        return promise;
    }

    public void addToProxies() {
        if (template().templateType() == TemplateType.SERVER) {
            ProxyRegisterServerPacket.create(this).broadcastPacket(TemplateType.SERVER);
        }
    }

    public void removeFromProxies() {
        if (template().templateType() == TemplateType.SERVER) {
            ProxyUnregisterServerPacket.create(this).broadcastPacket(TemplateType.SERVER);
        }
    }

    public void sync() {
        List<ClientboundPacket> syncPackets = new ArrayList<>(List.of(
                LanguageSyncPacket.fromLanguage(),
                LibrarySyncPacket.fromLibraries(this),
                ModuleSyncPacket.fromModuleCache(),
                MaintenanceListSyncPacket.fromMaintenanceListCache(),
                NotificationListSyncPacket.fromNotificationListCache(),
                BulkSyncPacket.generate()
        ));

        if (template().templateType().isProxy()) {
            for (CloudServer subServer : CloudServerManager.instance().getAll(TemplateType.SERVER)) {
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

    public CompletableFuture<Void> sendDelayedPacket(ClientboundPacket packet, long delayMs) {
        if (client().isEmpty()) return CompletableFuture.failedFuture(new RuntimeException("Client is empty"));
        return client().get().sendDelayedPacket(packet, delayMs);
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
        ServerSyncPacket.create(this, false).broadcastPacket();
    }

    public String name() {
        return templateName + "-" + id;
    }

    public Template template() {
        return TemplateManager.instance().get(templateName).orElseThrow(() -> new RuntimeException("Template null? This should not happen"));
    }

    public Path path() {
        if (template().settings().isStaticServers()) return PocketCloudPaths.storage().staticServers().with(name()).asPath();
        return PocketCloudPaths.tmp().with(uuid.toString()).asPath();
    }

    public Path logFilePath() {
        return path().resolve(template().serverSoftware().config().relativeLogFileLocation());
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
        return ServerClientCache.instance().get(this);
    }

    public List<CloudPlayer> players() {
        return CloudPlayerManager.instance().getAll(this);
    }

    public int playerCount() {
        return players().size();
    }

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static CloudServer read(Map<String, Object> data) {
        return MapperUtils.fromMap(data, CloudServer.class);
    }
}