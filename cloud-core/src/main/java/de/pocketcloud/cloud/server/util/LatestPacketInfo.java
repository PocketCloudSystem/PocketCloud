package de.pocketcloud.cloud.server.util;

import de.pocketcloud.network.packet.Packet;
import lombok.Getter;

@Getter
public final class LatestPacketInfo {

    private volatile Long receivedTime = null;
    private volatile Class<? extends Packet> receivedPacketClass = null;

    public synchronized void setLatestPacket(Long receivedTime, Class<? extends Packet> receivedPacketClass) {
        synchronized (this) {
            this.receivedTime = receivedTime;
            this.receivedPacketClass = receivedPacketClass;
        }
    }
}