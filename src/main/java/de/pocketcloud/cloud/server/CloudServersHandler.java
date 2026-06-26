package de.pocketcloud.cloud.server;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.packet.type.ServerCommandExecutionResult;
import de.pocketcloud.cloud.network.packet.type.ServerDisconnectReason;
import de.pocketcloud.cloud.server.util.ServerStatus;
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

    public static void handleDisconnect(@NotNull CloudServer server, ServerDisconnectReason reason) {

    }

    public static void handleCommandResponse(@NotNull CloudServer server, ServerCommandExecutionResult result) {

    }
}