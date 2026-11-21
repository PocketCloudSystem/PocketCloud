package de.pocketcloud.cloud.cluster;

public enum NodeState {
    /** Just started, collecting the view of the other nodes. */
    JOINING,
    /** Part of the cluster, another node is head. */
    FOLLOWER,
    /** Asking the other nodes to become head. */
    CANDIDATE,
    /** This node is head. */
    HEAD,
    /** Sees no majority: does not start or stop servers, running servers keep running. */
    ISOLATED
}
