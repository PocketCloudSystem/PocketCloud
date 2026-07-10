package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.notification.Notifier;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.type.NotificationType;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.player.CloudPlayerManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@NoArgsConstructor
@Getter
public final class CloudNotificationPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket, AuthenticatedPacket {

    private NotificationType notificationType;
    private Map<String, Object> args;

    public CloudNotificationPacket(NotificationType notificationType, Map<String, Object> args) {
        this.notificationType = notificationType;
        this.args = args != null ? args : Map.of();
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        if (Notifier.canLog(notificationType)) {
            switch (notificationType) {
                case PLAYER_JOIN_FAILED -> {
                    String player = (String) args.get("player");
                    String server = (String) args.get("server");
                    String reason = (String) args.get("reason");
                    boolean alreadyOnAServer = CloudPlayerManager.instance().get(player).filter(p -> p.currentServerName() != null).isPresent();
                    CloudLogger.get().info("The player §b{} §rtried to join" + (alreadyOnAServer ? "" : " via") + " §b{}§r, but got §ckicked§r: §b{}", player, server, formatReason(reason));
                }
                case PLAYER_KICKED -> {
                    String player = (String) args.get("player");
                    String server = (String) args.get("server");
                    String reason = (String) args.get("reason");
                    CloudLogger.get().info("The player §b{} §rhas been §ckicked §rfrom §b{}§r: §b{}", player, server, formatReason(reason));
                }
                default -> {}
            }
        }

        Notifier.notify(notificationType, args, Map.of());
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(notificationType, args);
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.notificationType = packetData.readEnum(NotificationType.class);
        this.args = packetData.readMap();
    }

    private String formatReason(String reason) {
        if (reason == null || reason.isEmpty()) return "No reason applied.";
        String newReason = reason.split("\n")[0];
        if (newReason.length() > 100) newReason = newReason.substring(0, 100);
        if (newReason.length() != reason.length()) newReason += "...";
        return newReason;
    }

    public static CloudNotificationPacket create(NotificationType notificationType, Map<String, Object> args) {
        return new CloudNotificationPacket(notificationType, args);
    }
}
