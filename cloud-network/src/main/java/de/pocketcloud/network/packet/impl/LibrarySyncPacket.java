package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

@NoArgsConstructor
@Getter
public final class LibrarySyncPacket extends CloudPacket implements ClientboundPacket {

    private Collection<LinkedHashMap<String, String>> data;

    public LibrarySyncPacket(Collection<LinkedHashMap<String, String>> data) {
        this.data = data != null ? data : List.of();
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(data);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static LibrarySyncPacket create(Collection<LinkedHashMap<String, String>> data) {
        return new LibrarySyncPacket(data);
    }
}