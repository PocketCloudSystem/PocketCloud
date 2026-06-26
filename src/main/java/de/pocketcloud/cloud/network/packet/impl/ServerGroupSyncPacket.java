package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class ServerGroupSyncPacket extends CloudPacket implements ClientboundPacket {

    private ServerGroup group;
    private boolean removal;

    public ServerGroupSyncPacket(ServerGroup group, boolean removal) {
        this.group = group;
        this.removal = removal;
    }

    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(group, removal);
    }

    @Override
    public void decodePayload(PacketData packetData) {}

    public static ServerGroupSyncPacket create(ServerGroup group, boolean removal) {
        return new ServerGroupSyncPacket(group, removal);
    }
}
