package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@NoArgsConstructor
@Getter
public final class BulkSyncPacket extends CloudPacket implements ClientboundPacket {

    private Collection<CloudServer> servers;
    private Collection<Template> templates;
    private Collection<CloudPlayer> players;
    private Collection<ServerGroup> groups;

    public BulkSyncPacket(Collection<CloudServer> servers, Collection<Template> templates, Collection<CloudPlayer> players, Collection<ServerGroup> groups) {
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

    public static BulkSyncPacket create(Collection<CloudServer> servers, Collection<Template> templates, Collection<CloudPlayer> players, Collection<ServerGroup> groups) {
        return new BulkSyncPacket(servers, templates, players, groups);
    }

    public static BulkSyncPacket generate() {
        return create(
            PocketCloud.instance().servers().getAll(),
            PocketCloud.instance().templates().getAll(),
            PocketCloud.instance().players().getAll(),
            PocketCloud.instance().serverGroups().getAll()
        );
    }
}
