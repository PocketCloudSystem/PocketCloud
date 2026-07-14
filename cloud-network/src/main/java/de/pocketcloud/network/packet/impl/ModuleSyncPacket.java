package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

@NoArgsConstructor
@Getter
public final class ModuleSyncPacket extends CloudPacket implements ClientboundPacket {

    private Collection<String> enabledModules;

    public ModuleSyncPacket(Collection<String> enabledModules) {
        this.enabledModules = enabledModules != null ? enabledModules : List.of();
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(enabledModules);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static ModuleSyncPacket create(Collection<String> enabledModules) {
        return new ModuleSyncPacket(enabledModules);
    }
}