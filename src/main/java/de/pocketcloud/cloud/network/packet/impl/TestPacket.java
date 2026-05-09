package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.util.PacketData;
import lombok.NoArgsConstructor;

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
        CloudLogger.get().info("TestPacket handle: {}", message);
    }
}