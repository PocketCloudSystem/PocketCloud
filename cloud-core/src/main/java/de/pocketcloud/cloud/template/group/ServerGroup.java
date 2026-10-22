package de.pocketcloud.cloud.template.group;

import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
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

    @MapCreator
    public ServerGroup(
            @MapKey(name = "name") String name,
            @MapKey(name = "templates") Collection<String> templates
    ) {
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
        SyncPacket.create(SyncType.SERVER_GROUP, data -> data.writeAll(this, markedForRemoval)).broadcast();
    }

    public Path path() {
        return PocketCloudPaths.groups().with(name).asPath();
    }

    public static ServerGroup read(Map<String, Object> map) {
        return MapperUtils.fromMap(map, ServerGroup.class);
    }
}