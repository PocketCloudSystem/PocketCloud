package de.pocketcloud.cloud.cluster.state;

/**
 * Logical keys used in the {@link ClusterStateStore}. Matches the key layout of the cluster spec
 * (without the {@code pc:{cloud}:} prefix, which remote stores add themselves).
 */
public final class StateKeys {

    /** Hash: server name → server document (incl. {@code node}). */
    public static final String SERVERS = "servers";

    /** Hash: player name → player document. */
    public static final String PLAYERS = "players";

    private StateKeys() {}

    /** Set: server ids that are taken for a template. */
    public static String ids(String template) {
        return "ids:" + template;
    }

    /** Set: names of the servers running on a node. */
    public static String nodeServers(String node) {
        return "node:" + node + ":servers";
    }
}
