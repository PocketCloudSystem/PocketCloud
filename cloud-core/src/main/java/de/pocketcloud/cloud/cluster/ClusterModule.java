package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.network.packet.CloudPacket;
import io.netty.channel.Channel;

import java.util.Collection;
import java.util.List;

/**
 * A feature that plugs into the node-to-node protocol (template replication, bridge/proxy sync, ...).
 * Modules are registered in {@link ClusterManager#modules()} and only run while the node is clustered.
 * All callbacks run on Netty or cluster scheduler threads: do blocking work on an own executor.
 */
public interface ClusterModule {

    /** Packet types this module adds. Names must start with {@code Node} or {@code Head} and be unique. */
    default Collection<Class<? extends Packet>> packets() {
        return List.of();
    }

    /** Called once the cluster network is up. */
    default void start(ClusterManager cluster) {
    }

    /** Called on shutdown, before the cluster network closes. */
    default void stop() {
    }

    /** A packet from an authenticated member. Return true if this module handled it. */
    default boolean onPacket(String from, Channel channel, CloudPacket packet) throws Exception {
        return false;
    }

    /** A node connected (or reconnected) and completed the hello. */
    default void onPeerConnected(String name) {
    }

    /** A node left or is considered dead. */
    default void onPeerGone(String name) {
    }
}
