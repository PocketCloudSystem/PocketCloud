package de.pocketcloud.cloud.network.packet.impl.request;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.RequestPacket;
import de.pocketcloud.cloud.network.packet.type.ActionFailureReason;
import de.pocketcloud.cloud.network.packet.impl.response.ServerStartResponsePacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.server.CloudServerManager;
import de.pocketcloud.cloud.template.TemplateManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class ServerStartRequestPacket extends RequestPacket {

    private String template;
    private int count;

    public ServerStartRequestPacket(String template, int count) {
        this.template = template != null ? template : "";
        this.count = count;
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        var tmpl = TemplateManager.instance().get(template).orElse(null);
        if (tmpl != null) {
            if (CloudServerManager.instance().getAll(tmpl).size() < tmpl.settings().getMaxServerCount()) {
                CloudServerManager.instance().start(tmpl, count);
                sendResponse(ServerStartResponsePacket.create(ActionFailureReason.NONE), client);
            } else {
                sendResponse(ServerStartResponsePacket.create(ActionFailureReason.MAX_SERVERS_REACHED), client);
            }
        } else {
            sendResponse(ServerStartResponsePacket.create(ActionFailureReason.TEMPLATE_NOT_FOUND), client);
        }
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.template = packetData.readString();
        this.count = packetData.readInt();
    }

    public static ServerStartRequestPacket create(String template, int count) {
        return new ServerStartRequestPacket(template, count);
    }
}
