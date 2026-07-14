package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(tps, avgTps, memoryUsage, memoryPeak, memoryLimit, cpuUsage);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
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
