package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.type.LogType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ConsoleLogPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket, AuthenticatedPacket {

    private String message;
    private LogType logType;

    public ConsoleLogPacket(String message, LogType logType) {
        this.message = message != null ? message : "";
        this.logType = logType;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(message, logType);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        this.message = packetData.readString();
        this.logType = packetData.readEnum(LogType.class);
    }

    public static ConsoleLogPacket create(String message, LogType logType) {
        return new ConsoleLogPacket(message, logType);
    }
}
