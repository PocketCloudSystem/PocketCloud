package de.pocketcloud.cloud.console;

import de.pocketcloud.cloud.PocketCloud;

public final class CloudShutdownHook extends Thread {

    public CloudShutdownHook() {
        super("Shutdown-Hook");
    }

    @Override
    public void run() {
        PocketCloud.instance().shutdown();
    }
}