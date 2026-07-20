package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.shared.network.packet.type.ServerDisconnectReason;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class DisconnectPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket, AuthenticatedPacket {

    private ServerDisconnectReason reason;

    public DisconnectPacket(ServerDisconnectReason reason) {
        this.reason = reason;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(reason);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        this.reason = packetData.readEnum(ServerDisconnectReason.class);
    }

    public static DisconnectPacket create(ServerDisconnectReason reason) {
        return new DisconnectPacket(reason);
    }
}
