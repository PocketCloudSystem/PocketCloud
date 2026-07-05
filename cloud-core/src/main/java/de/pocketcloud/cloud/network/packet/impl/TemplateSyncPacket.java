package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.template.Template;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class TemplateSyncPacket extends CloudPacket implements ClientboundPacket {

    private Template template;
    private boolean removal;

    public TemplateSyncPacket(Template template, boolean removal) {
        this.template = template;
        this.removal = removal;
    }

    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(template, removal);
    }

    @Override
    public void decodePayload(PacketData packetData) {}

    public static TemplateSyncPacket create(Template template, boolean removal) {
        return new TemplateSyncPacket(template, removal);
    }
}
