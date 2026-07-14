package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.model.player.ICloudPlayer;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerSyncPacket extends CloudPacket implements ClientboundPacket {

    private ICloudPlayer player;
    private boolean removal;

    public PlayerSyncPacket(ICloudPlayer player, boolean removal) {
        this.player = player;
        this.removal = removal;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(MapperUtils.toMap(player), removal);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static PlayerSyncPacket create(ICloudPlayer player, boolean removal) {
        return new PlayerSyncPacket(player, removal);
    }
}
