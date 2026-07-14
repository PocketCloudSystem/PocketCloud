package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.type.TextType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerTextPacket extends CloudPacket implements CloudboundPacket, ClientboundPacket, AuthenticatedPacket {

    private String player;
    private String text;
    private TextType type;

    public PlayerTextPacket(String player, String text, TextType type) {
        this.player = player != null ? player : "";
        this.text = text != null ? text : "";
        this.type = type;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(player, text, type);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        this.player = packetData.readString();
        this.text = packetData.readString();
        this.type = packetData.readEnum(TextType.class);
    }

    public static PlayerTextPacket create(String player, String text, TextType type) {
        return new PlayerTextPacket(player, text, type);
    }
}
