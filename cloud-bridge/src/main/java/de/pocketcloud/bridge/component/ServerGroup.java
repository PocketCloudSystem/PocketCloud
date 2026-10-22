package de.pocketcloud.bridge.component;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import de.pocketcloud.shared.component.BaseServerGroup;

import java.util.Collection;

public final class ServerGroup extends BaseServerGroup implements SyncingElement<IServerGroup> {

    @MapCreator
    public ServerGroup(
            @MapKey(name = "name") String name,
            @MapKey(name = "templates") Collection<String> templates
    ) {
        super(name, templates);
    }

    @Override
    public void syncIn(IServerGroup data) {
        templates(data.templates());
    }

    @Override
    public void syncOut() {}
}