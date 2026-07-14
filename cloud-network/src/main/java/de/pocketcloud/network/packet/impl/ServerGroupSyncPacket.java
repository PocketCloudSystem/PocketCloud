package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerGroupSyncPacket extends CloudPacket implements ClientboundPacket {

    private IServerGroup group;
    private boolean removal;

    public ServerGroupSyncPacket(IServerGroup group, boolean removal) {
        this.group = group;
        this.removal = removal;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(group, removal);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static ServerGroupSyncPacket create(IServerGroup group, boolean removal) {
        return new ServerGroupSyncPacket(group, removal);
    }
}
