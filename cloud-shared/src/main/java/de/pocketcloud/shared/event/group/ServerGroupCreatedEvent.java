package de.pocketcloud.shared.event.group;

import de.pocketcloud.api.component.group.IServerGroup;

public final class ServerGroupCreatedEvent extends ServerGroupEvent {

    public ServerGroupCreatedEvent(IServerGroup serverGroup) {
        super(serverGroup);
    }

    public IServerGroup getServerGroup() {
        return serverGroup;
    }
}