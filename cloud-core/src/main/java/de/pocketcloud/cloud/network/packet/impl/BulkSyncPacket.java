package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.player.CloudPlayerManager;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.CloudServerManager;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.TemplateManager;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.template.group.ServerGroupManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
public final class BulkSyncPacket extends CloudPacket implements ClientboundPacket {

    private List<CloudServer> servers;
    private List<Template> templates;
    private List<CloudPlayer> players;
    private List<ServerGroup> groups;

    public BulkSyncPacket(List<CloudServer> servers, List<Template> templates, List<CloudPlayer> players, List<ServerGroup> groups) {
        this.servers = servers != null ? servers : List.of();
        this.templates = templates != null ? templates : List.of();
        this.players = players != null ? players : List.of();
        this.groups = groups != null ? groups : List.of();
    }

    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void encodePayload(PacketData packetData) {
        List<Object> serverData = new ArrayList<>();
        for (CloudServer server : servers) serverData.add(server.write());

        List<Object> templateData = new ArrayList<>();
        for (Template template : templates) templateData.add(template.write());

        List<Object> playerData = new ArrayList<>();
        for (CloudPlayer player : players) playerData.add(player.write());

        List<Object> groupData = new ArrayList<>();
        for (ServerGroup group : groups) groupData.add(group.write());

        packetData.writeAll(serverData, templateData, playerData, groupData);
    }

    @Override
    public void decodePayload(PacketData packetData) {}

    public static BulkSyncPacket create(List<CloudServer> servers, List<Template> templates, List<CloudPlayer> players, List<ServerGroup> groups) {
        return new BulkSyncPacket(servers, templates, players, groups);
    }

    public static BulkSyncPacket generate() {
        return create(
            CloudServerManager.instance().getAll(),
            TemplateManager.instance().getAll(),
            CloudPlayerManager.instance().getAll(),
            ServerGroupManager.instance().getAll()
        );
    }
}
