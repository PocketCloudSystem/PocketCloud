package de.pocketcloud.cloud.server.software;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.util.FileUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ServerSoftwareManager {

    public static final List<ServerSoftware> DEFAULTS = List.of(
            new ServerSoftware("pmmp-latest", "SERVER", new ServerSoftware.SoftwareDownload(
                    "https://github.com/pmmp/PocketMine-MP/releases/latest/download/PocketMine-MP.phar",
                    "pmmp-latest.phar",
                    "{BINARY_PATH} {SOFTWARE_PATH}pmmp-latest.phar --no-wizard",
                    true
            ), new ServerSoftware.SoftwareBinary(
                    "https://github.com/pmmp/PHP-Binaries/releases/download/pm5-php-8.4-latest/PHP-8.4-Linux-x86_64-PM5.tar.gz",
                    true
            ), new ServerSoftware.SoftwareBridge(
                    "https://github.com/PocketCloudSystem/CloudBridge/releases/latest/download/CloudBridge.phar",
                    "plugins/CloudBridge.phar",
                    true
            ), new ServerSoftware.SoftwareConfig(
                    "server.properties",
                    "server.log",
                    List.of(),
                    "save-all"
            )),
            new ServerSoftware("waterdogpe-latest", "PROXY", new ServerSoftware.SoftwareDownload(
                    "https://github.com/WaterdogPE/WaterdogPE/releases/download/latest/Waterdog.jar",
                    "waterdog.jar",
                    "java -jar {SOFTWARE_PATH}waterdog.jar",
                    true
            ), new ServerSoftware.SoftwareBinary(
                    null,
                    false
            ), new ServerSoftware.SoftwareBridge(
                    "https://github.com/PocketCloudSystem/CloudBridge-Proxy/releases/latest/download/CloudBridge.jar",
                    "plugins/CloudBridge.jar",
                    true
            ), new ServerSoftware.SoftwareConfig(
                    "config.yml",
                    "logs/latest.log",
                    List.of(),
                    null
            ))
    );

    private final Map<String, ServerSoftware> softwareList = new HashMap<>();
    private final List<String> disabledSoftware = new ArrayList<>();

    public void load() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of("storage/software/"), "*.json")) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    ServerSoftware software = null;
                    try {
                        software = FileUtils.decodeJsonFile(file.toString(), ServerSoftware.class);
                        if (!software.normalizedName().equals(software.normalizedName())) {
                            CloudLogger.get().warn("Mismatch of name and file name from software {}", file.getFileName().toString());
                            continue;
                        }

                        loadSoftware(software);
                    } catch (Exception e) {
                        CloudLogger.get().exception("Failed to load software {}", e, file.getFileName().toString());
                        disabledSoftware.add(software == null ? file.getFileName().toString() : software.name());
                    }
                }
            }
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to load software list", e);
            PocketCloud.getInstance().shutdown();
            return;
        }

        for (ServerSoftware software : DEFAULTS) {
            if (!softwareList.containsKey(software.name())) {
                loadSoftware(software);
            }
        }
    }

    public void loadSoftware(ServerSoftware software) {
        CloudLogger.get().debug("Loaded {}, part of {} template type", software.name(), software.templateType());
        softwareList.put(software.name(), software);
        Objects.requireNonNull(software.type()).add(software);
        if (!software.directoryPath().toFile().exists() && !software.directoryPath().toFile().mkdirs()) throw new RuntimeException("Unable to create directory");
        if (!software.bridge().directoryPath(software).toFile().exists() && !software.bridge().directoryPath(software).toFile().mkdirs()) throw new RuntimeException("Unable to create directory");

        if (software.requiresUpdate()) {
            if (!software.downloadSoftware()) throw new RuntimeException("Failed to download software");
        }

        if (software.bridge().requiresUpdate(software)) {
            if (!software.bridge().download(software)) throw new RuntimeException("Failed to download software bridge");
        }

        if (software.binary().requiresUpdate(software)) {
            if (!software.binary().download(software)) throw new RuntimeException("Failed to download software binary");
        }
    }

    public void register(ServerSoftware software, boolean override) {
        if (softwareList.containsKey(software.name()) && !override) throw new IllegalArgumentException("ServerSoftware already exists");
        softwareList.put(software.name(), software);
        if (!software.directoryPath().toFile().exists() && !software.directoryPath().toFile().mkdirs()) throw new RuntimeException("Unable to create directory");
        if (!software.bridge().directoryPath(software).toFile().exists() && !software.bridge().directoryPath(software).toFile().mkdirs()) throw new RuntimeException("Unable to create directory");
        FileUtils.filePutContents(PocketCloudPaths.storage().software().with(software.normalizedName() + ".json").toString(), FileUtils.PRETTY_GSON.toJson(software));
        disabledSoftware.add(software.name());
        CloudLogger.get().warn("Please restart the cloud to download the required artifacts for the software §b{}§r.", software.name());
    }

    public void register(ServerSoftware software) {
        register(software, false);
    }

    public void unregister(ServerSoftware software) {
        softwareList.remove(software.name());
        disabledSoftware.remove(software.name());
    }

    public ServerSoftware get(String name) {
        return softwareList.getOrDefault(name, null);
    }

    public boolean disabled(ServerSoftware software) {
        return disabledSoftware.contains(software.name());
    }

    public Map<String, ServerSoftware> getAll() {
        return softwareList;
    }
}