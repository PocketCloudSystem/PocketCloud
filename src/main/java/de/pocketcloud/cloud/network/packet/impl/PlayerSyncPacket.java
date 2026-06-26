package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.player.CloudPlayer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class PlayerSyncPacket extends CloudPacket implements ClientboundPacket {

    private CloudPlayer player;
    private boolean removal;

    public PlayerSyncPacket(CloudPlayer player, boolean removal) {
        this.player = player;
        this.removal = removal;
    }

    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(player, removal);
    }

    @Override
    public void decodePayload(PacketData packetData) {}

    public static PlayerSyncPacket create(CloudPlayer player, boolean removal) {
        return new PlayerSyncPacket(player, removal);
    }
}
