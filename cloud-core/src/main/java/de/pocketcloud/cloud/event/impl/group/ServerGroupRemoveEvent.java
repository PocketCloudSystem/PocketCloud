package de.pocketcloud.cloud.event.impl.group;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.template.group.ServerGroup;

public class ServerGroupRemoveEvent extends ServerGroupEvent implements Cancelable {

    public ServerGroupRemoveEvent(ServerGroup serverGroup) {
        super(serverGroup);
    }
}