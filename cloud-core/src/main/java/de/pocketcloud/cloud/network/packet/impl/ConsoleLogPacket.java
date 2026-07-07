package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.type.LogType;
import de.pocketcloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
    public void handle(@NotNull ServerClient client) {
        client.server().logger().log(logType.toLogLevel(), message);
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(message, logType);
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.message = packetData.readString();
        this.logType = packetData.readEnum(LogType.class);
    }

    public static ConsoleLogPacket create(String message, LogType logType) {
        return new ConsoleLogPacket(message, logType);
    }
}
