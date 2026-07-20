package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.shared.network.packet.type.TextType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerTextPacket extends CloudPacket implements CloudboundPacket, ClientboundPacket, AuthenticatedPacket {

    private String player;
    private String title;
    private String body;
    private TextType type;
    private int fadeIn = 20;
    private int stay = 20;
    private int fadeOut = 5;

    public PlayerTextPacket(String player, String message, TextType type) {
        this.player = player;
        this.title = "";
        this.body = message;
        this.type = type;
    }

    public PlayerTextPacket(String player, String title, String body, TextType type) {
        this.player = player;
        this.title = title;
        this.body = body;
        this.type = type;
    }

    public PlayerTextPacket(String player, String title, String body, TextType type, int fadeIn, int stay, int fadeOut) {
        this.player = player;
        this.title = title;
        this.body = body;
        this.type = type;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(player, title, body, type, fadeIn, stay, fadeOut);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        this.player = packetData.readString();
        this.title = packetData.readString();
        this.body = packetData.readString();
        this.type = packetData.readEnum(TextType.class);
        this.fadeIn = packetData.readInt();
        this.stay = packetData.readInt();
        this.fadeOut = packetData.readInt();
    }

    public static PlayerTextPacket create(String player, String message, TextType type) {
        return new PlayerTextPacket(player, message, type);
    }

    public static PlayerTextPacket create(String player, String title, String body, TextType type) {
        return new PlayerTextPacket(player, title, body, type);
    }

    public static PlayerTextPacket create(String player, String title, String body, TextType type, int fadeIn, int stay, int fadeOut) {
        return new PlayerTextPacket(player, title, body, type, fadeIn, stay, fadeOut);
    }
}
