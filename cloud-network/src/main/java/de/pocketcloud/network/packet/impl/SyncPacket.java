package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.shared.sync.SyncType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.function.Consumer;

@NoArgsConstructor
@Getter
public final class SyncPacket extends CloudPacket implements CloudboundPacket, ClientboundPacket, AuthenticatedPacket {

    private SyncType syncType;
    private Consumer<IPacketData> dataAppender;
    private IPacketData remainingData;

    public SyncPacket(SyncType syncType, Consumer<IPacketData> dataAppender) {
        this.syncType = syncType;
        this.dataAppender = dataAppender;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(syncType);
        dataAppender.accept(packetData);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        syncType = SyncType.get(packetData.readString());
        remainingData = packetData.copyRemaining();
    }

    public static SyncPacket create(SyncType syncType, Consumer<IPacketData> dataAppender) {
        return new SyncPacket(syncType, dataAppender);
    }
}