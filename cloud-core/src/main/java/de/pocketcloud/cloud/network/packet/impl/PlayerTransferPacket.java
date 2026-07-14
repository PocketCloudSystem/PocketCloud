package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.player.CloudPlayer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class PlayerTransferPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket, AuthenticatedPacket {

    private String player;
    private String server;

    public PlayerTransferPacket(String player, String server) {
        this.player = player != null ? player : "";
        this.server = server != null ? server : "";
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        PocketCloud.instance().players().get(player).flatMap(CloudPlayer::currentProxy).ifPresent(proxy -> proxy.sendPacket(this));
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(player, server);
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.player = packetData.readString();
        this.server = packetData.readString();
    }

    public static PlayerTransferPacket create(String player, String server) {
        return new PlayerTransferPacket(player, server);
    }
}
