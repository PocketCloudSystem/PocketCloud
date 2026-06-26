package de.pocketcloud.cloud.event.impl.serverGroup;

import de.pocketcloud.cloud.event.impl.group.ServerGroupEvent;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import lombok.Getter;

import java.util.List;

public final class ServerGroupEditEvent extends ServerGroupEvent {

    @Getter
    private final List<Template> newTemplates;

    public ServerGroupEditEvent(ServerGroup serverGroup, List<Template> newTemplates) {
        super(serverGroup);
        this.newTemplates = newTemplates;
    }
}
