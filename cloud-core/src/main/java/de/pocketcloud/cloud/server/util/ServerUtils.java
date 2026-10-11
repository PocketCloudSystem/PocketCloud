package de.pocketcloud.cloud.server.util;

import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.common.util.NetUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class ServerUtils {

    private static final Object LOCK = new Object();
    private static final Map<String, Set<Integer>> ids = new HashMap<>();
    private static final Set<Integer> usedPorts = new HashSet<>();

    private ServerUtils() {}

    public static int reserveId(ITemplate template) {
        synchronized (LOCK) {
            Set<Integer> templateIds = ids.computeIfAbsent(template.name(), _ -> new HashSet<>());
            for (int i = 1; i <= template.settings().maxServerCount(); i++) {
                if (templateIds.add(i)) return i;
            }
            return -1;
        }
    }

    public static void addId(ITemplate template, int id) {
        synchronized (LOCK) {
            ids.computeIfAbsent(template.name(), _ -> new HashSet<>()).add(id);
        }
    }

    public static void removeId(ITemplate template, int id) {
        synchronized (LOCK) {
            Set<Integer> templateIds = ids.get(template.name());
            if (templateIds != null) templateIds.remove(id);
        }
    }

    public static int reservePort(TemplateType type) {
        ServerPortRange portRange = TemplateTypeHelper.serverPortRange(type);
        int start = portRange.start();
        int lastV4 = portRange.end() - 1;
        if (lastV4 < start) return 0;

        synchronized (LOCK) {
            if (portRange.random()) {
                for (int tries = 0; tries < 30; tries++) {
                    int port = start + ThreadLocalRandom.current().nextInt(lastV4 - start + 1);
                    if (tryReserve(port)) return port;
                }
            }

            for (int port = start; port <= lastV4; port++) {
                if (tryReserve(port)) return port;
            }
            return 0;
        }
    }

    private static boolean tryReserve(int port) {
        int portV6 = port + 1;
        if (usedPorts.contains(port) || usedPorts.contains(portV6)) return false;
        if (!NetUtils.isLocalUdpPortFree(port) || !NetUtils.isLocalUdpPortFree(portV6)) return false;

        usedPorts.add(port);
        usedPorts.add(portV6);
        return true;
    }

    public static void removePort(int port) {
        synchronized (LOCK) {
            usedPorts.remove(port);
            usedPorts.remove(port + 1);
        }
    }
}