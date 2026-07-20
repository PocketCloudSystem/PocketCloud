package de.pocketcloud.bridge.component;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.shared.component.BaseServerGroup;

import java.util.Collection;

public final class ServerGroup extends BaseServerGroup implements SyncingElement<IServerGroup> {

    public ServerGroup(String name, Collection<String> templates) {
        super(name, templates);
    }

    @Override
    public void syncIn(IServerGroup data) {
        templates(data.templates());
    }

    @Override
    public void syncOut() {}
}