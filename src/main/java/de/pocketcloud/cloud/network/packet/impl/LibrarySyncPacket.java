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
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
public final class LibrarySyncPacket extends CloudPacket implements ClientboundPacket {

    private List<Map<String, String>> data;

    public LibrarySyncPacket(List<Map<String, String>> data) {
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

    public static LibrarySyncPacket create(List<Map<String, String>> data) {
        return new LibrarySyncPacket(data);
    }

    public static LibrarySyncPacket fromLibraries(CloudServer server) {
        List<Map<String, String>> data = new ArrayList<>();
        for (Library lib : LibraryManager.instance().getAll().values()) {
            if (!lib.isAvailableFor(server.template().serverSoftware())) continue;
            data.add(Map.of(
                "name", lib.name(),
                "path", lib.directoryPath().toAbsolutePath().toString(),
                "namespacePrefix", lib.namespacePrefix(),
                "namespaceFolder", lib.namespaceFolder()
            ));
        }
        return new LibrarySyncPacket(data);
    }
}
