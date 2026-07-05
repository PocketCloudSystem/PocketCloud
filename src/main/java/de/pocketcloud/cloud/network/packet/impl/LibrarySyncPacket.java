package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.library.Library;
import de.pocketcloud.cloud.server.library.LibraryManager;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@NoArgsConstructor
@Getter
public final class LibrarySyncPacket extends CloudPacket implements ClientboundPacket {

    private List<LinkedHashMap<String, String>> data;

    public LibrarySyncPacket(List<LinkedHashMap<String, String>> data) {
        this.data = data != null ? data : List.of();
    }

    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(data);
    }

    @Override
    public void decodePayload(PacketData packetData) {}

    public static LibrarySyncPacket create(List<LinkedHashMap<String, String>> data) {
        return new LibrarySyncPacket(data);
    }

    public static LibrarySyncPacket fromLibraries(CloudServer server) {
        List<LinkedHashMap<String, String>> data = new ArrayList<>();
        for (Library lib : LibraryManager.instance().getAll().values()) {
            if (!lib.isAvailableFor(server.template().serverSoftware())) continue;
            LinkedHashMap<String, String> libData = new LinkedHashMap<>();
            libData.put("name", lib.name());
            libData.put("path", lib.directoryPath().toAbsolutePath().toString());
            libData.put("namespacePrefix", lib.namespacePrefix());
            libData.put("namespaceFolder", lib.namespaceFolder());
            data.add(libData);
        }
        return new LibrarySyncPacket(data);
    }
}