package de.pocketcloud.network.packet.impl.response;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerNotificationCheckResponsePacket extends ResponsePacket implements ClientboundPacket {

    private boolean enabled;

    public PlayerNotificationCheckResponsePacket(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(enabled);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static PlayerNotificationCheckResponsePacket create(boolean enabled) {
        return new PlayerNotificationCheckResponsePacket(enabled);
    }
}
