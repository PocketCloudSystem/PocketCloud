package de.pocketcloud.network.packet.impl.response;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import de.pocketcloud.shared.network.packet.type.ActionFailureReason;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;

@NoArgsConstructor
@Getter
public final class ServerStartResponsePacket extends ResponsePacket implements ClientboundPacket {

    private ActionFailureReason errorReason;
    private Collection<String> startedServers;

    public ServerStartResponsePacket(ActionFailureReason errorReason, Collection<String> startedServers) {
        this.errorReason = errorReason;
        this.startedServers = startedServers;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(errorReason, startedServers);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        errorReason = packetData.readEnum(ActionFailureReason.class);
        startedServers = packetData.readArray(String.class);
    }

    public static ServerStartResponsePacket create(ActionFailureReason errorReason,  Collection<String> startedServers) {
        return new ServerStartResponsePacket(errorReason, startedServers);
    }
}
