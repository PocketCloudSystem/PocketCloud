package de.pocketcloud.cloud.server.config;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.config.impl.PowerNukkitXCloudConfig;
import de.pocketcloud.cloud.server.config.impl.PowerNukkitXConfig;
import de.pocketcloud.cloud.server.config.impl.WaterdogConfig;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ServerPropertiesGenerator implements Loadable {

    private final Map<String, List<IServerProperties>> defaultConfigFiles = new HashMap<>();

    @Override
    public void load() {
        register(new PowerNukkitXConfig());
        register(new PowerNukkitXCloudConfig());
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
            Path globalTemplatePath = TemplateTypeHelper.globalTemplatePath(type);
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

    public List<IServerProperties> getAll(IServerSoftware software) {
        if (software == null) return defaultConfigFiles.values().stream().flatMap(List::stream).toList();
        return defaultConfigFiles.getOrDefault(software.name(), null);
    }
}