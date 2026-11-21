package de.pocketcloud.api.cluster;

/**
 * A node (machine) of the cloud. Without a cluster there is exactly one node.
 */
public interface INode {

    String name();

    /**
     * Address under which other nodes (and proxies) reach this node.
     */
    String address();

    /**
     * Port for node-to-node connections.
     */
    int port();

    /**
     * Membership state as seen by the cloud: HEAD, FOLLOWER, JOINING, ISOLATED or DEAD.
     */
    String state();

    /**
     * Whether this node is the head (it schedules the servers of the cluster).
     */
    boolean head();

    default boolean alive() {
        return !"DEAD".equals(state());
    }
}
