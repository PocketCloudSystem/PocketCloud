package de.pocketcloud.cloud.event.impl.group;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.template.group.ServerGroup;

public class ServerGroupCreateEvent extends ServerGroupEvent implements Cancelable {

    public ServerGroupCreateEvent(ServerGroup serverGroup) {
        super(serverGroup);
    }
}