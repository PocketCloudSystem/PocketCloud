package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.cache.ActiveInGameModuleCache;
import de.pocketcloud.cloud.cache.LocalCache;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@NoArgsConstructor
@Getter
public final class ModuleSyncPacket extends CloudPacket implements ClientboundPacket {

    private List<String> enabledModules;

    public ModuleSyncPacket(List<String> enabledModules) {
        this.enabledModules = enabledModules != null ? enabledModules : List.of();
    }

    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(enabledModules);
    }

    @Override
    public void decodePayload(PacketData packetData) {}

    public static ModuleSyncPacket create(List<String> enabledModules) {
        return new ModuleSyncPacket(enabledModules);
    }

    public static ModuleSyncPacket fromModuleCache() {
        return new ModuleSyncPacket(LocalCache.get(ActiveInGameModuleCache.class).getAll().stream().toList());
    }
}