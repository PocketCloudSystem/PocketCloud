package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class KeepAlivePacket extends CloudPacket implements ClientboundPacket, CloudboundPacket, AuthenticatedPacket {

    private double tps;
    private double avgTps;
    private double memoryUsage;
    private double memoryPeak;
    private double memoryLimit;
    private double cpuUsage;

    public KeepAlivePacket(double tps, double avgTps, double memoryUsage, double memoryPeak, double memoryLimit, double cpuUsage) {
        this.tps = tps;
        this.avgTps = avgTps;
        this.memoryUsage = memoryUsage;
        this.memoryPeak = memoryPeak;
        this.memoryLimit = memoryLimit;
        this.cpuUsage = cpuUsage;
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        var server = client.server();
        server.lastKeepAlive(System.currentTimeMillis() / 1000L);
        server.serverData().setPerformanceStats(tps, avgTps, memoryUsage, memoryPeak, memoryLimit, cpuUsage);
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(tps, avgTps, memoryUsage, memoryPeak, memoryLimit, cpuUsage);
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.tps = packetData.readDouble();
        this.avgTps = packetData.readDouble();
        this.memoryUsage = packetData.readDouble();
        this.memoryPeak = packetData.readDouble();
        this.memoryLimit = packetData.readDouble();
        this.cpuUsage = packetData.readDouble();
    }

    public static KeepAlivePacket create(double tps, double avgTps, double memoryUsage, double memoryPeak, double memoryLimit, double cpuUsage) {
        return new KeepAlivePacket(tps, avgTps, memoryUsage, memoryPeak, memoryLimit, cpuUsage);
    }
}
