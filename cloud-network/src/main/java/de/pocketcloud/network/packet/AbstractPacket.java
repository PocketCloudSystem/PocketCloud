package de.pocketcloud.network.packet;

import de.pocketcloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.Setter;

public abstract class AbstractPacket implements Packet {

    @Getter
    private boolean encoded = false;
    @Getter
    private Long sentTimestamp = null;
    @Setter
    @Getter
    private long size = 0;

    @Override
    public void encode(PacketData packetData) {
        if (encoded) throw new RuntimeException("Packet " + getName() + " has already been encoded");
        encoded = true;
        packetData.write(getName()).write(sentTimestamp = System.currentTimeMillis());
        encodePayload(packetData);
    }

    @Override
    public void decode(PacketData packetData) {
        String packetName = packetData.readString();
        assert packetName != null;
        if (!packetName.equals(getName())) throw new RuntimeException("...");
        sentTimestamp = packetData.readLong();
        decodePayload(packetData);
    }

    @Override
    public final String getName() {
        return getClass().getSimpleName();
    }
}