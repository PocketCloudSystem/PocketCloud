package de.pocketcloud.cloud.event.impl.group;

import de.pocketcloud.cloud.event.Event;
import de.pocketcloud.cloud.template.group.ServerGroup;
import lombok.Getter;

public abstract class ServerGroupEvent extends Event {

    @Getter
    private final ServerGroup serverGroup;

    public ServerGroupEvent(ServerGroup serverGroup) {
        this.serverGroup = serverGroup;
    }
}