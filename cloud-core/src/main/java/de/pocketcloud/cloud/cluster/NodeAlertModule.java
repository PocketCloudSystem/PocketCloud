package de.pocketcloud.cloud.cluster;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.event.impl.node.HeadChangedEvent;
import de.pocketcloud.cloud.event.impl.node.NodeConnectedEvent;
import de.pocketcloud.cloud.event.impl.node.NodeDisconnectedEvent;

import java.awt.Color;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Node events for cloud plugins on every node, and Discord alerts ({@code node-alerts}) sent by one node only:
 * the head, or while there is no head (no majority) the oldest reachable node, so each change is reported once.
 */
public final class NodeAlertModule implements ClusterModule {

    /** Nodes this node saw go away (every node tracks them, so a new head knows them too). */
    private final Set<String> gone = ConcurrentHashMap.newKeySet();
    private ClusterManager cluster;
    private volatile String head;

    @Override
    public void start(ClusterManager cluster) {
        this.cluster = cluster;
    }

    @Override
    public void onPeerConnected(String name) {
        new NodeConnectedEvent(name).call();
        if (gone.remove(name) && reporter()) {
            alert("Node back online", "`" + name + "` has reconnected to the cluster.", Color.GREEN);
        }
    }

    @Override
    public void onPeerGone(String name) {
        if (!gone.add(name)) return; // reported by several paths
        new NodeDisconnectedEvent(name).call();
        if (reporter()) {
            alert("Node offline", "`" + name + "` is not reachable. Its servers are treated as offline and replaced where needed.", Color.RED);
        }
    }

    @Override
    public void onHeadChanged(String newHead) {
        String previous = head;
        head = newHead;
        if (newHead == null || newHead.equals(previous)) return;
        boolean self = newHead.equals(cluster.nodeName());
        new HeadChangedEvent(newHead, previous, self).call();
        if (self && previous != null) {
            String unreachable = gone.isEmpty() ? "" : "\nUnreachable: `" + String.join("`, `", gone) + "`";
            alert("Head changed", "`" + newHead + "` is the new head (before: `" + previous + "`)." + unreachable, Color.ORANGE);
        }
    }

    private boolean reporter() {
        if (cluster.isHead()) return true;
        return cluster.head() == null && cluster.nodeName().equals(cluster.oldest(cluster.aliveNodes(System.currentTimeMillis())));
    }

    /** Discord alert of this node (if {@code node-alerts} is enabled); also used for certificate warnings. */
    public static void alert(String title, String description, Color color) {
        PocketCloud.instance().notifications().sendNodeAlert(title, description, color);
    }
}
