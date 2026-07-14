package de.pocketcloud.cloud.server.util;

import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.cloud.config.ServerSettingsConfig;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.common.util.NetUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ServerUtils {
    
    private static final Map<String, Set<Integer>> ids = new HashMap<>();
    private static final Set<Integer> usedPorts = new HashSet<>();

    private ServerUtils() {}

    public static void addId(ITemplate template, int id) {
        ids.computeIfAbsent(template.name(), k -> new HashSet<>()).add(id);
    }

    public static void removeId(ITemplate template, int id) {
        Set<Integer> templateIds = ids.get(template.name());
        if (templateIds != null) templateIds.remove(id);
    }

    public static int getFreeId(ITemplate template) {
        Set<Integer> templateIds = ids.computeIfAbsent(template.name(), k -> new HashSet<>());
        for (int i = 1; i <= template.settings().maxServerCount(); i++) {
            if (!templateIds.contains(i)) return i;
        }
        return -1;
    }

    public static void addPort(int port) {
        usedPorts.add(port);
    }

    public static void removePort(int port) {
        usedPorts.remove(port);
    }

    public static int getFreePort(TemplateType type) {
        ServerSettingsConfig.ServerPortRange portRange = TemplateTypeHelper.serverPortRange(type);
        int start = portRange.start();
        int end = portRange.end();
        boolean randomPorts = portRange.random();

        int currentPort = start;

        for (int tries = 0; tries < 30; tries++) {
            int port = randomPorts
                ? start + (int) (Math.random() * (end - start + 1))
                : currentPort++;
            int portV6 = port + 1;

            if (!usedPorts.contains(port) && !usedPorts.contains(portV6) &&
                NetUtils.isLocalUdpPortFree(port) && NetUtils.isLocalUdpPortFree(portV6)) {
                return port;
            }
        }

        return 0;
    }
}