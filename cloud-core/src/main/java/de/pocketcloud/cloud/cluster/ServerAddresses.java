package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.config.sub.ServerAddressesConfiguration;
import de.pocketcloud.cloud.console.log.CloudLogger;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Addresses of the servers started on this node.
 * <ul>
 *     <li><b>public address</b>: how proxies and servers reach a server. Sent to the proxies
 *     ({@code ProxyRegisterServerPacket}). In a cluster the proxy may run on another machine, so a loopback
 *     {@code addresses.public-address} is replaced by the {@code advertisedAddress} of this node.</li>
 *     <li><b>bind address</b>: what the server listens on. In a cluster a loopback bind address is not reachable
 *     from proxies on other machines; it is widened to {@code 0.0.0.0} if this node is advertised under
 *     a non-loopback address.</li>
 * </ul>
 * Single-node setups and setups where every node is advertised as loopback (one machine) are unchanged.
 */
public final class ServerAddresses {

    private static final AtomicBoolean WARNED_BIND = new AtomicBoolean();

    private ServerAddresses() {
    }

    public static String publicAddress() {
        String configured = config().publicAddress();
        String advertised = advertisedAddress();
        if (advertised != null && isLoopback(configured) && !isLoopback(advertised)) return advertised;
        return configured;
    }

    public static String bindAddress(TemplateType type) {
        String configured = config().bindAddress(type);
        String advertised = advertisedAddress();
        if (advertised != null && isLoopback(configured) && !isLoopback(advertised)) {
            if (WARNED_BIND.compareAndSet(false, true)) {
                CloudLogger.get().warn("The bind address §b{} §ris not reachable from other nodes, servers bind to §b0.0.0.0 §rinstead. " +
                        "Set §baddresses.server-bind-address §rin server_settings.yml to choose an address.", configured);
            }
            return "0.0.0.0";
        }
        return configured;
    }

    /** The advertised address of this node, or null if it does not run in a cluster. */
    private static String advertisedAddress() {
        ClusterManager cluster = PocketCloud.instance().cluster();
        if (cluster == null || !cluster.clustered()) return null;
        String advertised = PocketCloud.instance().nodeConfig().advertisedAddress();
        return advertised == null || advertised.isBlank() ? null : advertised.trim();
    }

    private static ServerAddressesConfiguration config() {
        return PocketCloud.instance().serverSettingsConfig().addresses();
    }

    static boolean isLoopback(String address) {
        if (address == null || address.isBlank()) return false;
        String a = address.trim();
        if (a.equals("localhost") || a.startsWith("127.") || a.equals("::1") || a.equals("[::1]")) return true;
        try {
            // Only literal addresses are checked: no DNS lookups here.
            if (a.matches("[0-9.]+") || a.contains(":")) return InetAddress.getByName(a).isLoopbackAddress();
        } catch (Exception ignored) {
        }
        return false;
    }
}
