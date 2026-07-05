package de.pocketcloud.cloud.server.config;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.cloud.server.config.impl.PocketMineConfig;
import de.pocketcloud.cloud.server.config.impl.PocketMineServerProperties;
import de.pocketcloud.cloud.server.config.impl.WaterdogConfig;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.template.TemplateType;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ServerPropertiesGenerator implements Loadable {

    @Getter
    @Accessors(fluent = true)
    private static ServerPropertiesGenerator instance;

    private final Map<String, List<IServerProperties>> defaultConfigFiles = new HashMap<>();

    public ServerPropertiesGenerator() {
        instance = this;
    }

    @Override
    public void load() {
        register(new PocketMineConfig());
        register(new PocketMineServerProperties());
        register(new WaterdogConfig());
    }

    @Override
    public void unload() {
        defaultConfigFiles.clear();
    }

    public void register(IServerProperties properties) {
        defaultConfigFiles
            .computeIfAbsent(properties.getServerSoftware().name(), k -> new ArrayList<>())
            .add(properties);

        for (TemplateType type : TemplateType.values()) {
            if (!properties.getServerSoftware().templateType().equals(type.name())) continue;
            Path globalTemplatePath = type.globalTemplatePath();
            FileUtils.createDir(globalTemplatePath);

            Path propertiesPath = globalTemplatePath.resolve(properties.getFileName());
            if (!Files.exists(propertiesPath) || properties.needsRenewal(propertiesPath.toString())) {
                CloudLogger.get().info("Updating server properties/config for {}: {}...", type.name(), properties.getFileName());
                properties.renew(propertiesPath.toString());

                int i = 0;
                try (Stream<Path> dirs = Files.list(PocketCloudPaths.templates().asPath())) {
                    for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                        if (dir.getFileName().toString().equals("global")) continue;
                        Path filePath = dir.resolve(properties.getFileName());
                        if (Files.exists(filePath) && properties.needsRenewal(filePath.toString())) {
                            properties.renew(filePath.toString());
                            i++;
                        }
                    }
                } catch (IOException e) {
                    CloudLogger.get().error("Failed to update template properties", e);
                }

                if (i > 0) {
                    CloudLogger.get().info("Also updating server properties/config for {} templates: {}...", i, properties.getFileName());
                }
            }
        }
    }

    public void remove(IServerProperties properties) {
        defaultConfigFiles.get(properties.getServerSoftware().name()).remove(properties);
    }

    public List<IServerProperties> getAll(ServerSoftware software) {
        if (software == null) return defaultConfigFiles.values().stream().flatMap(List::stream).toList();
        return defaultConfigFiles.getOrDefault(software.name(), null);
    }
}