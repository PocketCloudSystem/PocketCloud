package de.pocketcloud.cloud.server.software;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.ToNumberPolicy;
import de.pocketcloud.api.component.software.*;
import de.pocketcloud.api.provider.write.IWriteSoftwareProvider;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.shared.component.software.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ServerSoftwareManager implements IWriteSoftwareProvider, Loadable {

    private static final Gson SOFTWARE_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
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
            new ServerSoftware("powernukkitx-latest", "SERVER", new SoftwareDownload(
                    "https://github.com/PowerNukkitX/PowerNukkitX/releases/latest/download/powernukkitx.jar",
                    "powernukkitx.jar",
                    new String[]{
                            "java",
                            "-Dfile.encoding=UTF-8",
                            "-Djansi.passthrough=true",
                            "-Xmx{MAX_MEMORY}M",
                            "-XX:+UseZGC",
                            "-XX:+ZGenerational",
                            "-XX:+UseStringDeduplication",
                            "-XX:ActiveProcessorCount=2",
                            "-XX:CICompilerCount=2",
                            "-XX:ConcGCThreads=1",
                            "-XX:+UnlockExperimentalVMOptions",
                            "-XX:SoftMaxHeapSize=1024M",
                            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                            "--add-opens", "java.base/java.io=ALL-UNNAMED",
                            "--add-opens", "java.base/java.net=ALL-UNNAMED",
                            "-jar",
                            "{SOFTWARE_PATH}powernukkitx.jar"
                    },
                    true
            ), new SoftwareBinary(
                    null,
                    false
            ), new SoftwareBridge(
                    "https://github.com/PocketCloudSystem/PocketCloud/releases/download/latest-bridge/cloudbridge.jar",
                    "plugins/cloudbridge.jar",
                    true
            ), new SoftwareConfig(
                    "config.yml",
                    "logs/server.log",
                    List.of("command_data", "players", "resource_packs", "worlds", "structures", "services", "banned-ips.json", "banned-players.json", "ops.txt", "white-list.txt"),
                    "save-all"
            )),
            new ServerSoftware("waterdogpe-latest", "PROXY", new SoftwareDownload(
                    "https://github.com/WaterdogPE/WaterdogPE/releases/download/latest/Waterdog.jar",
                    "waterdog.jar",
                    new String[]{
                            "java",
                            "-Dfile.encoding=UTF-8",
                            "-Xmx{MAX_MEMORY}M",
                            "-XX:+UseG1GC",
                            "-XX:MaxGCPauseMillis=100",
                            "-XX:ActiveProcessorCount=1",
                            "-XX:CICompilerCount=2",
                            "-XX:ParallelGCThreads=2",
                            "-XX:ConcGCThreads=1",
                            "-jar",
                            "{SOFTWARE_PATH}waterdog.jar"
                    },
                    true
            ), new SoftwareBinary(
                    null,
                    false
            ), new SoftwareBridge(
                    "https://github.com/PocketCloudSystem/PocketCloud/releases/download/latest-bridge/cloudbridge.jar",
                    "plugins/cloudbridge.jar",
                    true
            ), new SoftwareConfig(
                    "config.yml",
                    "logs/server.log",
                    List.of("packs", "lang.ini"),
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
        if (!service().directoryPath(software).toFile().exists() && !service().directoryPath(software).toFile().mkdirs())
            throw new RuntimeException("Unable to create directory");
        if (!service().bridgeDirectoryPath(software).toFile().exists() && !service().bridgeDirectoryPath(software).toFile().mkdirs())
            throw new RuntimeException("Unable to create directory");

        if (!service().configFilePath(software).toFile().exists()) {
            try (BufferedWriter writer = Files.newBufferedWriter(service().configFilePath(software), StandardCharsets.UTF_8)) {
                SOFTWARE_GSON.toJson(software, writer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

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

    @Override
    public void register(IServerSoftware software, boolean override) {
        ServerSoftware serverSoftware = requireServerSoftware(software);
        if (softwareList.containsKey(serverSoftware.name()) && !override)
            throw new IllegalArgumentException("ServerSoftware already exists");
        softwareList.put(serverSoftware.name(), serverSoftware);
        if (!service().directoryPath(serverSoftware).toFile().exists() && !service().directoryPath(serverSoftware).toFile().mkdirs())
            throw new RuntimeException("Unable to create directory");
        if (!service().bridgeDirectoryPath(serverSoftware).toFile().exists() && !service().bridgeDirectoryPath(serverSoftware).toFile().mkdirs())
            throw new RuntimeException("Unable to create directory");

        try (BufferedWriter writer = Files.newBufferedWriter(service().configFilePath(serverSoftware), StandardCharsets.UTF_8)) {
            SOFTWARE_GSON.toJson(serverSoftware, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        disabledSoftware.add(serverSoftware.name());
        CloudLogger.get().warn("Please restart the cloud to download the required artifacts for the software §b{}§r.", serverSoftware.name());
    }

    @Override
    public void unregister(IServerSoftware software) {
        softwareList.remove(software.name());
        disabledSoftware.remove(software.name());
    }

    @Override
    public boolean check(String name) {
        return softwareList.containsKey(name);
    }

    @Override
    public Optional<IServerSoftware> get(String name) {
        return Optional.ofNullable(softwareList.get(name));
    }

    public boolean disabled(ServerSoftware software) {
        return disabledSoftware.contains(software.name());
    }

    public SoftwareService service() {
        return PocketCloud.instance().software();
    }

    @Override
    public Collection<IServerSoftware> getAll() {
        return widen(softwareList.values().stream().toList());
    }

    @SuppressWarnings("unchecked")
    private <T extends IServerSoftware> Collection<IServerSoftware> widen(Collection<T> collection) {
        return (Collection<IServerSoftware>) collection;
    }

    private ServerSoftware requireServerSoftware(IServerSoftware serverSoftware) {
        if (!(serverSoftware instanceof ServerSoftware software)) {
            throw new IllegalArgumentException("Unsupported IServerSoftware implementation: " + serverSoftware.getClass().getName());
        }

        return software;
    }
}