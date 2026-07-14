package de.pocketcloud.cloud.template.util;

import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.config.ServerSettingsConfig;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TemplateTypeHelper {

    private static final Map<TemplateType, List<ServerSoftware>> softwareList = new HashMap<>();

    public static Path globalTemplatePath(TemplateType type) {
        return PocketCloudPaths.templates().global().with(type.name().toLowerCase()).asPath();
    }

    public static ServerSettingsConfig.ServerPortRange serverPortRange(TemplateType type) {
        return PocketCloud.instance().serverSettingsConfig().getServerPortRange(type);
    }

    public static int timeout(TemplateType type) {
        return PocketCloud.instance().serverSettingsConfig().getServerTimeout(type);
    }

    public static void addSoftware(TemplateType type, ServerSoftware software) {
        if (!softwareList.containsKey(type)) softwareList.put(type, new ArrayList<>());
        softwareList.get(type).add(software);
    }

    public static void removeSoftware(TemplateType type, ServerSoftware software) {
        if (softwareList.containsKey(type)) softwareList.get(type).remove(software);
    }

    public static @Nullable ServerSoftware getSoftware(TemplateType type, String name) {
        if (!softwareList.containsKey(type)) return null;
        return softwareList.get(type).stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
    }

    public static List<ServerSoftware> softwareList(TemplateType templateType) {
        return softwareList.getOrDefault(templateType, new ArrayList<>());
    }
}