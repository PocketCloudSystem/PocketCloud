package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public final class HeadForwardStartResultPacket extends ResponsePacket {

    private List<String> servers;

    public HeadForwardStartResultPacket(List<String> servers) {
        this.servers = servers;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.write(servers);
    }

    @Override
    public void decodePayload(IPacketData data) {
        servers = data.readArray(String.class);
    }
}
