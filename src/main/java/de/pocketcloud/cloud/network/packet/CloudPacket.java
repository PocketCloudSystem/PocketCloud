package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.packet.util.PacketData;

public abstract class CloudPacket implements Packet {
    
    private boolean encoded = false;
    private Long sentTimestamp = null;
    
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
        if (!packetName.equals(getName())) throw new RuntimeException("Packet name does not equal the actual class name? What have you done?");
        sentTimestamp = packetData.readLong();
        if (sentTimestamp == null) throw new RuntimeException("Packet data does not contain the actual sent timestamp? What have you done?");
        decodePayload(packetData);
    }

    public void sendPacket() {
        if (!(this instanceof CloudboundPacket)) return;
        //Network.getInstance().sendPacket((CloudboundPacket) this);
        //TODO
    }
    
    @Override
    public abstract void handle();
    
    @Override
    public final String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public final boolean isEncoded() {
        return encoded;
    }
    
    @Override
    public final Long getSentTimestamp() {
        return sentTimestamp;
    }
}
