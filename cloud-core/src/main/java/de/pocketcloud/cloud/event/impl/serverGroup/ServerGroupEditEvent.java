package de.pocketcloud.cloud.event.impl.serverGroup;

import de.pocketcloud.cloud.event.impl.group.ServerGroupEvent;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import lombok.Getter;

import java.util.Collection;

@Getter
public class ServerGroupEditEvent extends ServerGroupEvent {

    private final Collection<Template> oldTemplates;
    private final Collection<Template> newTemplates;

    public ServerGroupEditEvent(ServerGroup serverGroup, Collection<Template> oldTemplates, Collection<Template> newTemplates) {
        super(serverGroup);
        this.oldTemplates = oldTemplates;
        this.newTemplates = newTemplates;
    }
}