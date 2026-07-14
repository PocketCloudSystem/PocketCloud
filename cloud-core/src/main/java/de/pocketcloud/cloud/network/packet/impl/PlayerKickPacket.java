package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.player.CloudPlayerManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class PlayerKickPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket, AuthenticatedPacket {

    private String player;
    private String reason;
    private String disconnectScreenMessage;

    public PlayerKickPacket(String player, String reason, String disconnectScreenMessage) {
        this.player = player != null ? player : "";
        this.reason = reason != null ? reason : "";
        this.disconnectScreenMessage = disconnectScreenMessage != null ? disconnectScreenMessage : "";
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        PocketCloud.instance().players().get(player).ifPresent(p -> p.kick(reason, disconnectScreenMessage));
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(player, reason, disconnectScreenMessage);
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.player = packetData.readString();
        this.reason = packetData.readString();
        this.disconnectScreenMessage = packetData.readString();
    }

    public static PlayerKickPacket create(String player, String reason, String disconnectScreenMessage) {
        return new PlayerKickPacket(player, reason, disconnectScreenMessage);
    }
}
