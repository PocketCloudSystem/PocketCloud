package de.pocketcloud.cloud.network.packet.handler;

import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.util.CloudLogLevelHelper;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.impl.CloudNotificationPacket;
import de.pocketcloud.network.packet.impl.ConsoleLogPacket;

import java.util.Map;

public final class NormalPacketHandler implements PacketListener {

    @PacketHandler({ConsoleLogPacket.class})
    public void handle(ConsoleLogPacket packet, ServerClient sender) {
        sender.server().logger().log(CloudLogLevelHelper.toLogLevel(packet.getLogType()), packet.getMessage());
    }

    @PacketHandler({CloudNotificationPacket.class})
    public void handle(CloudNotificationPacket packet, ServerClient sender) {
        if (PocketCloud.instance().notifications().canLog(packet.getNotificationType())) {
            switch (packet.getNotificationType()) {
                case PLAYER_JOIN_FAILED -> {
                    String player = (String) packet.getArgs().get("player");
                    String server = (String) packet.getArgs().get("server");
                    String reason = (String) packet.getArgs().get("reason");
                    boolean alreadyOnAServer = PocketCloud.instance().players().get(player).filter(p -> p.currentServerName() != null).isPresent();
                    CloudLogger.get().info("The player §b{} §rtried to join" + (alreadyOnAServer ? "" : " via") + " §b{}§r, but got §ckicked§r: §b{}", player, server, formatReason(reason));
                }
                case PLAYER_KICKED -> {
                    String player = (String) packet.getArgs().get("player");
                    String server = (String) packet.getArgs().get("server");
                    String reason = (String) packet.getArgs().get("reason");
                    CloudLogger.get().info("The player §b{} §rhas been §ckicked §rfrom §b{}§r: §b{}", player, server, formatReason(reason));
                }
                default -> {}
            }
        }

        PocketCloud.instance().notifications().notify(packet.getNotificationType(), packet.getArgs(), Map.of());
    }

    private String formatReason(String reason) {
        String firstPart = reason.split("\n")[0];
        String formattedPart = firstPart.trim().substring(0, 100);
        return formattedPart + (firstPart.length() > formattedPart.length() ? "..." : "");
    }
}
