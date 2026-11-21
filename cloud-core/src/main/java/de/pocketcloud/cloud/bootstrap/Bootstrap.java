package de.pocketcloud.cloud.bootstrap;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;


public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--join") && i + 1 < args.length) {
                ClusterManager.storeJoinToken(args[i + 1]);
            }
        }

        new PocketCloud();
    }
}