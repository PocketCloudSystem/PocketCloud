package de.pocketcloud.cloud.server;

import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.server.ServerDisconnectEvent;
import de.pocketcloud.network.packet.type.ServerDisconnectReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CloudServersHandler {

    public static void handleStartSuccess(@NotNull CloudServer server, @Nullable Long tmpPid) {

    }

    public static void handleStartFailure(@NotNull CloudServer server, @Nullable Throwable e, boolean processFailure) {
        if (server.status() != ServerStatus.STARTING) return;
        if (!processFailure) server.kill();
        server.setStatus(ServerStatus.OFFLINE);
        if (e != null) {
            CloudLogger.get().exception("Failed to start server §b{}§r{}", e, server.name(), processFailure ? "§8, §rfailed to create process§r..." : "§8, deleting data...");
        } else {
            CloudLogger.get().warn("Failed to start server §b{}§r{}", server.name(), processFailure ? "§8, §rfailed to create process..." : "§8, §rdeleting data...");
        }

        server.remove();
        server.deleteTmpDir();
    }

    public static void handleDisconnect(@NotNull CloudServer server, @NotNull ServerDisconnectReason reason) {
        if (server.status().isOffline()) {
            PocketCloud.instance().servers().remove(server);
            return;
        }

        server.setStatus(ServerStatus.OFFLINE);
        new ServerDisconnectEvent(server).call();
        CloudLogger.get().info("The server §b{} §rhas §cdisconnected §rfrom the cloud.", server.name());
        //todo check for crash

        server.remove();
        server.deleteTmpDir();
    }
}