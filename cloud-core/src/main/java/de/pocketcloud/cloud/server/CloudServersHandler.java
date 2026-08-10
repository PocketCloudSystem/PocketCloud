package de.pocketcloud.cloud.server;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.server.ServerCrashedEvent;
import de.pocketcloud.cloud.server.crash.CrashData;
import de.pocketcloud.shared.event.server.ServerDisconnectedEvent;
import de.pocketcloud.shared.event.server.ServerStartFailedEvent;
import de.pocketcloud.shared.event.server.ServerStopTimedOutEvent;
import de.pocketcloud.shared.event.server.ServerTimedOutEvent;
import de.pocketcloud.shared.network.packet.type.NotificationType;
import de.pocketcloud.shared.network.packet.type.ServerDisconnectReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class CloudServersHandler {

    public static void handleStartSuccess(@NotNull CloudServer server, @Nullable Long tmpPid) {}

    public static void handleStartFailure(@NotNull CloudServer server, @Nullable Throwable e, boolean processFailure) {
        if (server.status() != ServerStatus.STARTING) return;
        if (!processFailure) server.kill();
        server.status(ServerStatus.OFFLINE);
        String reason;
        if (e != null) {
            reason = e.getMessage();
            CloudLogger.get().exception("Failed to start server §b{}§r{}", e, server.name(), processFailure ? "§8, §rfailed to create process§r..." : "§8, deleting data...");
        } else {
            reason = processFailure ? "Failed to create process" : "";
            CloudLogger.get().warn("Failed to start server §b{}§r{}", server.name(), processFailure ? "§8, §rfailed to create process..." : "§8, §rdeleting data...");
        }

        PocketCloud.instance().notifications().sendNotification(NotificationType.SERVER_START_FAILED, Map.of("server", server.name(), "reason", reason), Map.of());
        CloudAPI.instance().events().call(new ServerStartFailedEvent(server, reason));

        server.remove();
        server.deleteTmpDir();
    }

    public static void handleTimeout(@NotNull CloudServer server) {
        if (!server.status().isOnline()) return;
        server.status(ServerStatus.OFFLINE);
        server.remove();
        server.kill();

        CloudAPI.instance().events().call(new ServerTimedOutEvent(server));
        if (!checkForCrash(server))
            CloudLogger.get().warn("The server §b{} §r§ctimed out§r, deleting data...", server.name());
        PocketCloud.instance().notifications().sendNotification(NotificationType.SERVER_TIMED_OUT, Map.of("server", server.name()), Map.of());

        server.deleteTmpDir();
    }

    public static void handleDisconnect(@NotNull CloudServer server, @NotNull ServerDisconnectReason reason) {
        if (server.status().isOffline()) {
            PocketCloud.instance().servers().remove(server);
            return;
        }

        server.status(ServerStatus.OFFLINE);
        CloudAPI.instance().events().call(new ServerDisconnectedEvent(server));
        CloudLogger.get().info("The server §b{} §rhas §cdisconnected §rfrom the cloud.", server.name());
        if (checkForCrash(server)) server.kill();

        server.remove();
        server.deleteTmpDir();
    }

    public static void handleStopTimeout(@NotNull CloudServer server) {
        if (!server.status().isStopping()) return;
        server.status(ServerStatus.OFFLINE);
        server.remove();
        server.kill();
        CloudAPI.instance().events().call(new ServerStopTimedOutEvent(server));
        CloudLogger.get().warn("Failed to stop §b{}§r, deleting data & killing process...", server.name());
        PocketCloud.instance().notifications().sendNotification(NotificationType.SERVER_STOP_TIMED_OUT, Map.of("server", server.name()), Map.of());

        server.deleteTmpDir();
    }

    public static boolean checkForCrash(@NotNull CloudServer server) {
        CrashData data = PocketCloud.instance().crashHandlers().retrieveCrashData(server);
        if (data.crashed()) {
            CloudLogger.get().warn("The server §b{} §ccrashed§r, writing crash file...", server.name());
            data.printStackTrace();
            data.writeFile();
            new ServerCrashedEvent(server, data).call();
            CloudAPI.instance().events().call(new de.pocketcloud.shared.event.server.ServerCrashedEvent(server));
            PocketCloud.instance().notifications().sendNotification(NotificationType.SERVER_CRASHED, Map.of("server", server.name()), Map.of("crashData", data.write()));
            return true;
        }
        return false;
    }
}