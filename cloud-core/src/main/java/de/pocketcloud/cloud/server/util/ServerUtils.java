package de.pocketcloud.cloud.server.util;

import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.state.ClusterStateStore;
import de.pocketcloud.cloud.cluster.state.StateKeys;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.common.util.NetUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class ServerUtils {

    private static final Object LOCK = new Object();
    private static final Set<Integer> usedPorts = new HashSet<>();

    private ServerUtils() {}

    /**
     * Reserves the lowest free id of a template in the shared state, so two nodes can never
     * hand out the same server name.
     */
    public static int reserveId(ITemplate template) {
        String key = StateKeys.ids(template.name());
        try {
            for (int i = 1; i <= template.settings().maxServerCount(); i++) {
                if (state().addToSet(key, String.valueOf(i))) return i;
            }
        } catch (IllegalStateException e) {
            CloudLogger.get().error("Cannot start a server of §b{}§r: {}", template.name(), e.getMessage());
        }
        return -1;
    }

    public static void addId(ITemplate template, int id) {
        state().addToSet(StateKeys.ids(template.name()), String.valueOf(id));
    }

    public static void removeId(ITemplate template, int id) {
        state().removeFromSet(StateKeys.ids(template.name()), String.valueOf(id));
    }

    private static ClusterStateStore state() {
        return PocketCloud.instance().cluster().state();
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

    /** Marks a port as used (a server process adopted after a restart already listens on it). */
    public static void claimPort(int port) {
        synchronized (LOCK) {
            usedPorts.add(port);
            usedPorts.add(port + 1);
        }
    }

    public static void removePort(int port) {
        synchronized (LOCK) {
            usedPorts.remove(port);
            usedPorts.remove(port + 1);
        }
    }
}