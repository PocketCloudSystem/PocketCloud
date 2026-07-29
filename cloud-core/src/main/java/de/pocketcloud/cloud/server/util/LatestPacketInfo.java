package de.pocketcloud.cloud.server.util;

import de.pocketcloud.api.network.packet.Packet;
import lombok.Getter;

import java.time.Instant;

@Getter
public final class LatestPacketInfo {

    private volatile Instant receivedTime = null;
    private volatile Class<? extends Packet> receivedPacketClass = null;

    public synchronized void setLatestPacket(Instant receivedTime, Class<? extends Packet> receivedPacketClass) {
        synchronized (this) {
            this.receivedTime = receivedTime;
            this.receivedPacketClass = receivedPacketClass;
        }
    }
}