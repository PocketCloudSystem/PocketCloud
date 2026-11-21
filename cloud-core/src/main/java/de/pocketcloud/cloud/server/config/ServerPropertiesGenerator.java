package de.pocketcloud.cloud.server.config;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.config.impl.PowerNukkitXConfig;
import de.pocketcloud.cloud.server.config.impl.PowerNukkitXNightlyConfig;
import de.pocketcloud.cloud.server.config.impl.WaterdogConfig;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.shared.component.software.ServerSoftware;

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
        register(new PowerNukkitXNightlyConfig());
        register(new WaterdogConfig());
    }

    @Override
    public void unload() {
        defaultConfigFiles.clear();
    }

    public void register(IServerProperties properties) {
        defaultConfigFiles.computeIfAbsent(properties.getServerSoftware().name(), k -> new ArrayList<>())
                .add(properties);

        ServerSoftware software = (ServerSoftware) properties.getServerSoftware();
        Path configDir = PocketCloud.instance().software().configDirectoryPath(software);
        FileUtils.createDir(configDir);

        String fileName = properties.getFileName();
        Path propertiesPath = configDir.resolve(fileName);

        Path legacyDir = TemplateTypeHelper.globalTemplatePath(TemplateType.valueOf(software.templateType()));
        try {
            Files.deleteIfExists(legacyDir.resolve(fileName));
            Files.deleteIfExists(legacyDir.resolve("." + fileName + ".version"));
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to remove legacy global config", e);
        }

        if (!PocketCloud.instance().config().checkProperties()) return;

        if (!Files.exists(propertiesPath) || properties.needsRenewal(propertiesPath.toString())) {
            CloudLogger.get().info("Updating config for {}: {}...", software.name(), fileName);
            properties.renew(propertiesPath.toString());
        }

        int i = 0;
        for (ITemplate t : PocketCloud.instance().templates().getAll()) {
            if (!t.serverSoftware().name().equals(software.name())) continue;
            Path filePath = ((Template) t).path().resolve(fileName);
            if (Files.exists(filePath) && properties.needsRenewal(filePath.toString())) {
                properties.renew(filePath.toString());
                i++;
            }
        }

        if (i > 0) CloudLogger.get().info("Also updating {} for {} templates...", fileName, i);
    }

    public void remove(IServerProperties properties) {
        defaultConfigFiles.get(properties.getServerSoftware().name()).remove(properties);
    }

    public List<IServerProperties> getAll(IServerSoftware software) {
        if (software == null) return defaultConfigFiles.values().stream().flatMap(List::stream).toList();
        return defaultConfigFiles.getOrDefault(software.name(), null);
    }
}