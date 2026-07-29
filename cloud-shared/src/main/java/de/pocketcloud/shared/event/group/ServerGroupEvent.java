package de.pocketcloud.shared.event.group;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.event.Event;

public abstract class ServerGroupEvent implements Event {

    protected final IServerGroup serverGroup;

    public ServerGroupEvent(IServerGroup serverGroup) {
        this.serverGroup = serverGroup;
    }
}