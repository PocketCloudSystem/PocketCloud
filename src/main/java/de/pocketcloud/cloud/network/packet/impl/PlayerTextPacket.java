package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.type.TextType;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.player.CloudPlayerManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class PlayerTextPacket extends CloudPacket implements CloudboundPacket, ClientboundPacket {

    private String player;
    private String text;
    private TextType type;

    public PlayerTextPacket(String player, String text, TextType type) {
        this.player = player != null ? player : "";
        this.text = text != null ? text : "";
        this.type = type;
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        CloudPlayerManager.instance().get(player).ifPresent(p -> p.send(text, type));
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(player, text, type);
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.player = packetData.readString();
        this.text = packetData.readString();
        this.type = packetData.readTextType();
    }

    public static PlayerTextPacket create(String player, String text, TextType type) {
        return new PlayerTextPacket(player, text, type);
    }
}
