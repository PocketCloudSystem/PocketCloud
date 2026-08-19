package de.pocketcloud.shared.event.group;

import de.pocketcloud.api.component.group.IServerGroup;

public final class ServerGroupDeletedEvent extends ServerGroupEvent {

    public ServerGroupDeletedEvent(IServerGroup serverGroup) {
        super(serverGroup);
    }
}