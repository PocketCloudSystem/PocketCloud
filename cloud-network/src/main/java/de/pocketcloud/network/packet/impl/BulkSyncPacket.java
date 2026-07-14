package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.model.player.ICloudPlayer;
import de.pocketcloud.api.model.server.ICloudServer;
import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
public final class BulkSyncPacket extends CloudPacket implements ClientboundPacket {

    private Collection<ICloudServer> servers;
    private Collection<ITemplate> templates;
    private Collection<ICloudPlayer> players;
    private Collection<IServerGroup> groups;

    public BulkSyncPacket(Collection<ICloudServer> servers, Collection<ITemplate> templates, Collection<ICloudPlayer> players, Collection<IServerGroup> groups) {
        this.servers = servers != null ? servers : List.of();
        this.templates = templates != null ? templates : List.of();
        this.players = players != null ? players : List.of();
        this.groups = groups != null ? groups : List.of();
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        List<Map<String, Object>> serverData = new ArrayList<>();
        for (ICloudServer server : servers) serverData.add(MapperUtils.toMap(server));

        List<Map<String, Object>> templateData = new ArrayList<>();
        for (ITemplate template : templates) templateData.add(MapperUtils.toMap(template));

        List<Map<String, Object>> playerData = new ArrayList<>();
        for (ICloudPlayer player : players) playerData.add(MapperUtils.toMap(player));

        List<Map<String, Object>> groupData = new ArrayList<>();
        for (IServerGroup group : groups) groupData.add(MapperUtils.toMap(group));

        packetData.writeAll(serverData, templateData, playerData, groupData);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static BulkSyncPacket create(Collection<ICloudServer> servers, Collection<ITemplate> templates, Collection<ICloudPlayer> players, Collection<IServerGroup> groups) {
        return new BulkSyncPacket(servers, templates, players, groups);
    }

    @SuppressWarnings("unchecked")
    public static BulkSyncPacket generate() {
        return create(
                (Collection<ICloudServer>) CloudAPI.instance().servers().getAll(),
                (Collection<ITemplate>) CloudAPI.instance().templates().getAll(),
                (Collection<ICloudPlayer>) CloudAPI.instance().players().getAll(),
                (Collection<IServerGroup>) CloudAPI.instance().serverGroups().getAll()
        );
    }
}
