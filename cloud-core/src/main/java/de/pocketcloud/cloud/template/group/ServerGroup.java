package de.pocketcloud.cloud.template.group;

import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.component.BaseServerGroup;
import de.pocketcloud.shared.sync.SyncType;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public final class ServerGroup extends BaseServerGroup implements SyncingElement<ServerGroup> {

    /**
     * Only meant for the SyncPacket
     */
    private transient boolean markedForRemoval = false;

    public ServerGroup(String name, Collection<String> templates) {
        super(name, templates);
    }

    public ServerGroup markForRemoval() {
        this.markedForRemoval = true;
        return this;
    }

    @Override
    public void syncIn(ServerGroup data) {}

    @Override
    public void syncOut() {
        SyncPacket.create(SyncType.SERVER_GROUP, data -> data.write(this), Map.of("removal", markedForRemoval)).broadcast();
    }

    public Path path() {
        return PocketCloudPaths.groups().with(name).asPath();
    }

    public static ServerGroup read(Map<String, Object> map) {
        return MapperUtils.fromMap(map, ServerGroup.class);
    }
}