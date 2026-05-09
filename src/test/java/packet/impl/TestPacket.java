package packet.impl;

import lombok.NoArgsConstructor;
import packet.CloudPacket;
import packet.util.PacketData;

@NoArgsConstructor
public final class TestPacket extends CloudPacket {

    private String message;

    public TestPacket(String message) {
        this.message = message;
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.write(message);
    }

    @Override
    public void decodePayload(PacketData packetData) {
        message = packetData.readString();
    }

    @Override
    public void handle() {
        System.out.println("TestPacket handle: " + message);
    }
}