package de.pocketcloud.cloud.event.impl.group;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import lombok.Getter;

public class ServerGroupRemoveTemplateEvent extends ServerGroupEvent implements Cancelable {

    @Getter
    private final Template template;

    public ServerGroupRemoveTemplateEvent(ServerGroup serverGroup, Template template) {
        super(serverGroup);
        this.template = template;
    }
}