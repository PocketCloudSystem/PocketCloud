package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class TemplateSyncPacket extends CloudPacket implements ClientboundPacket {

    private ITemplate template;
    private boolean removal;

    public TemplateSyncPacket(ITemplate template, boolean removal) {
        this.template = template;
        this.removal = removal;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(MapperUtils.toMap(template), removal);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static TemplateSyncPacket create(ITemplate template, boolean removal) {
        return new TemplateSyncPacket(template, removal);
    }
}
