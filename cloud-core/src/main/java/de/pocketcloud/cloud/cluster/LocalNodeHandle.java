package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.cluster.storage.LocalTemplateStorage;
import de.pocketcloud.cloud.cluster.storage.TemplateStorage;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.config.IServerProperties;
import de.pocketcloud.cloud.server.util.ServerStartMethods;
import de.pocketcloud.cloud.server.util.ServerUtils;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.common.util.ProcessUtils;
import de.pocketcloud.shared.component.software.ServerSoftware;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static de.pocketcloud.common.util.FileUtils.IO_EXECUTOR;

/**
 * The node of the machine this cloud process runs on.
 * Contains everything that touches local processes, ports and files of a server.
 */
public final class LocalNodeHandle implements NodeHandle {

    public static final String DEFAULT_NAME = "local";

    private final String name;

    public LocalNodeHandle(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean local() {
        return true;
    }

    @Override
    public int reservePort(TemplateType type) {
        return ServerUtils.reservePort(type);
    }

    @Override
    public void releasePort(int port) {
        ServerUtils.removePort(port);
    }

    @Override
    public Promise<Void> prepare(CloudServer server) {
        return Promise.runAsync(() -> {
            Path logFileLocation = server.logFilePath();
            if (Files.exists(logFileLocation)) {
                try {
                    FileTime ctime = Files.readAttributes(logFileLocation, BasicFileAttributes.class).creationTime();
                    String timestamp = DateTimeFormatter
                            .ofPattern("yyyy-MM-dd_HH:mm:ss.SSS_z")
                            .withZone(ZoneId.systemDefault())
                            .format(ctime.toInstant());
                    String archiveFileName = timestamp + "_" + logFileLocation.getFileName() + ".log";
                    storage().archiveLog(server.templateName(), archiveFileName, logFileLocation);
                } catch (IOException e) {
                    CloudLogger.get().exception("Failed to archive log file", e);
                }

                FileUtils.unlinkFile(logFileLocation);
            }

            if (Files.exists(server.path()) && !server.template().settings().staticServers() && server.template().settings().deleteOnStop()) {
                FileUtils.removeDirectory(server.path());
            }

            Path bridgePath = PocketCloud.instance().software().bridgeSourceFile((ServerSoftware) server.template().serverSoftware());
            Path bridgeDstPath = server.path().resolve(server.template().serverSoftware().bridge().relativeServerPath());
            if (!Files.isDirectory(bridgeDstPath.getParent())) FileUtils.createDir(bridgeDstPath.getParent());
            if (Files.isRegularFile(bridgePath)) {
                try {
                    Files.copy(bridgePath, bridgeDstPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    CloudLogger.get().exception("Failed to copy bridge", e);
                }
            }

            copy(() -> storage().copyGlobalTo(server.template().templateType(), server.path()));

            Path softwareConfigPath = PocketCloud.instance().software()
                    .configDirectoryPath((ServerSoftware) server.template().serverSoftware());
            FileUtils.copyDirectory(softwareConfigPath, server.path(), Set.of());

            boolean copyFromSources = server.template().settings().alwaysCopyToStaticServers() || !server.template().settings().staticServers();
            if (copyFromSources) {
                for (IServerGroup group : server.template().parentGroups()) {
                    copy(() -> storage().copyGroupTo(group.name(), server.path()));
                }

                copy(() -> storage().copyTemplateTo(server.templateName(), server.path(), Set.of(LocalTemplateStorage.LOG_ARCHIVE + "/")));
            }

            List<IServerProperties> propertiesData = PocketCloud.instance().properties().getAll(server.template().serverSoftware());
            if (propertiesData == null) {
                throw new RuntimeException("No server configuration(s) found");
            }

            for (var properties : propertiesData) {
                Path filePath = server.path().resolve(properties.getFileName());
                Map<String, Object> replacements = properties.replacePlaceholders(server);
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

                FileUtils.filePutContents(filePath, unquotePrimitives(content));
            }

            FileUtils.unlinkFile(logFileLocation);
        }, IO_EXECUTOR);
    }

    @Override
    public Promise<Optional<Long>> boot(CloudServer server) {
        return ServerStartMethods.current()
                .start(new CloudServer[]{server})
                .thenApply(pids -> Optional.ofNullable(pids.get(server.name())));
    }

    @Override
    public Promise<Optional<Long>> lookupPid(CloudServer server) {
        return ServerStartMethods.current().lookupPid(server);
    }

    @Override
    public boolean isProcessAlive(CloudServer server) {
        Long pid = server.data().processId();
        if (pid == null) return false;
        Optional<ProcessHandle> proc = ProcessHandle.of(pid);
        return proc.isPresent() && proc.get().isAlive();
    }

    @Override
    public boolean processExists(CloudServer server) {
        Long pid = server.data().processId();
        return pid != null && ProcessHandle.of(pid).isPresent();
    }

    @Override
    public void kill(CloudServer server) {
        Long pid = server.data().processId();
        if (pid != null) ProcessUtils.kill(pid, true);
    }

    @Override
    public Promise<Void> saveFiles(CloudServer server) {
        de.pocketcloud.cloud.cluster.sync.TemplateReplicator.get().ifPresent(r -> r.noteSave(server.templateName(), server.name()));
        return Promise.supplyAsync(() -> {
            for (String file : server.template().serverSoftware().config().savableFiles()) {
                Path filePath = server.path().resolve(file);
                copy(() -> storage().storeInTemplate(server.templateName(), file, filePath));
            }

            return null;
        }, IO_EXECUTOR);
    }

    @Override
    public Optional<List<String>> readLogs(CloudServer server) {
        Path logFilePath = server.logFilePath();
        if (!Files.exists(logFilePath)) return Optional.empty();

        try {
            return Optional.of(Files.readAllLines(logFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static TemplateStorage storage() {
        return PocketCloud.instance().cluster().templateStorage();
    }

    private static void copy(IoAction action) {
        try {
            action.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private static String unquotePrimitives(String content) {
        return content.replaceAll(
                "(?m)^(\\s*[\\w.\\-]+:\\s*)\"(-?\\d+(?:\\.\\d+)?|true|false)\"(\\s*(?:#.*)?)$",
                "$1$2$3"
        );
    }
}
