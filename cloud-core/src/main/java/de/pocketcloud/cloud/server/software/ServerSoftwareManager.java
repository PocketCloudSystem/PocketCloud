package de.pocketcloud.cloud.server.software;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.ToNumberPolicy;
import de.pocketcloud.api.component.software.*;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.shared.component.software.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ServerSoftwareManager implements Loadable {

    private static final Gson SOFTWARE_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(ISoftwareDownload.class,
                    (JsonDeserializer<ISoftwareDownload>) (json, _, ctx) -> ctx.deserialize(json, SoftwareDownload.class))
            .registerTypeAdapter(ISoftwareBinary.class,
                    (JsonDeserializer<ISoftwareBinary>) (json, _, ctx) -> ctx.deserialize(json, SoftwareBinary.class))
            .registerTypeAdapter(ISoftwareBridge.class,
                    (JsonDeserializer<ISoftwareBridge>) (json, _, ctx) -> ctx.deserialize(json, SoftwareBridge.class))
            .registerTypeAdapter(ISoftwareConfig.class,
                    (JsonDeserializer<ISoftwareConfig>) (json, _, ctx) -> ctx.deserialize(json, SoftwareConfig.class))
            .create();

    public static final List<ServerSoftware> DEFAULTS = List.of(
            new ServerSoftware("pmmp-latest", "SERVER", new SoftwareDownload(
                    "https://github.com/pmmp/PocketMine-MP/releases/latest/download/PocketMine-MP.phar",
                    "pmmp-latest.phar",
                    "{BINARY_PATH}bin/php7/bin/php {SOFTWARE_PATH}pmmp-latest.phar --no-wizard",
                    true
            ), new SoftwareBinary(
                    "https://github.com/pmmp/PHP-Binaries/releases/download/pm5-php-8.4-latest/PHP-8.4-Linux-x86_64-PM5.tar.gz",
                    true
            ), new SoftwareBridge(
                    "https://github.com/PocketCloudSystem/CloudBridge/releases/latest/download/CloudBridge.phar",
                    "plugins/CloudBridge.phar",
                    true
            ), new SoftwareConfig(
                    "server.properties",
                    "server.log",
                    List.of(),
                    "save-all"
            )),
            new ServerSoftware("waterdogpe-latest", "PROXY", new SoftwareDownload(
                    "https://github.com/WaterdogPE/WaterdogPE/releases/download/latest/Waterdog.jar",
                    "waterdog.jar",
                    "java -jar {SOFTWARE_PATH}waterdog.jar",
                    true
            ), new SoftwareBinary(
                    null,
                    false
            ), new SoftwareBridge(
                    "https://github.com/PocketCloudSystem/CloudBridge-Proxy/releases/latest/download/CloudBridge.jar",
                    "plugins/CloudBridge.jar",
                    true
            ), new SoftwareConfig(
                    "config.yml",
                    "logs/latest.log",
                    List.of(),
                    null
            ))
    );

    private final Map<String, ServerSoftware> softwareList = new HashMap<>();
    private final List<String> disabledSoftware = new ArrayList<>();

    @Override
    public void preload() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of("storage/software/"), "*.json")) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    ServerSoftware software = null;
                    try {
                        software = FileUtils.decodeJsonFile(file, ServerSoftware.class, SOFTWARE_GSON);
                        if (!software.normalizedName().equals(file.getFileName().toString().replace(".json", ""))) {
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
            PocketCloud.instance().shutdown();
            return;
        }

        for (ServerSoftware software : DEFAULTS) {
            if (!softwareList.containsKey(software.name())) {
                loadSoftware(software);
            }
        }
    }

    @Override
    public void load() {}

    public void loadSoftware(ServerSoftware software) {
        CloudLogger.get().debug("Loaded {}, part of {} template type", software.name(), software.templateType());
        softwareList.put(software.name(), software);
        TemplateTypeHelper.addSoftware(Objects.requireNonNull(software.type()), software);
        if (!service().directoryPath(software).toFile().exists() && !service().directoryPath(software).toFile().mkdirs()) throw new RuntimeException("Unable to create directory");
        if (!service().bridgeDirectoryPath(software).toFile().exists() && !service().bridgeDirectoryPath(software).toFile().mkdirs()) throw new RuntimeException("Unable to create directory");

        if (!service().configFilePath(software).toFile().exists()) FileUtils.encodeJsonFile(service().configFilePath(software), software, SOFTWARE_GSON);

        if (service().requiresUpdateSoftware(software)) {
            if (!service().downloadSoftware(software)) throw new RuntimeException("Failed to download software");
        }

        if (service().requiresUpdateBridge(software)) {
            if (!service().downloadBridge(software)) throw new RuntimeException("Failed to download software bridge");
        }

        if (service().requiresUpdateBinary(software)) {
            if (!service().downloadBinary(software)) throw new RuntimeException("Failed to download software binary");
        }
    }

    @Override
    public void unload() {
        softwareList.clear();
        disabledSoftware.clear();
    }

    public void register(ServerSoftware software, boolean override) {
        if (softwareList.containsKey(software.name()) && !override) throw new IllegalArgumentException("ServerSoftware already exists");
        softwareList.put(software.name(), software);
        if (!service().directoryPath(software).toFile().exists() && !service().directoryPath(software).toFile().mkdirs()) throw new RuntimeException("Unable to create directory");
        if (!service().bridgeDirectoryPath(software).toFile().exists() && !service().bridgeDirectoryPath(software).toFile().mkdirs()) throw new RuntimeException("Unable to create directory");
        FileUtils.encodeJsonFile(service().configFilePath(software), software, SOFTWARE_GSON);
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
        return softwareList.get(name);
    }

    public boolean disabled(ServerSoftware software) {
        return disabledSoftware.contains(software.name());
    }
    
    public SoftwareService service() {
        return PocketCloud.instance().software();
    }
    
    public Map<String, ServerSoftware> getAll() {
        return softwareList;
    }
}