package de.pocketcloud.network.packet.impl.response;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerWhitelistCheckResponsePacket extends ResponsePacket implements ClientboundPacket {

    private boolean whitelisted;

    public PlayerWhitelistCheckResponsePacket(boolean whitelisted) {
        this.whitelisted = whitelisted;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(whitelisted);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static PlayerWhitelistCheckResponsePacket create(boolean whitelisted) {
        return new PlayerWhitelistCheckResponsePacket(whitelisted);
    }
}
