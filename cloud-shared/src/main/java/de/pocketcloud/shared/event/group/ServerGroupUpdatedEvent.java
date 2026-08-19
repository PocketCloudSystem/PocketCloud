package de.pocketcloud.shared.event.group;

import de.pocketcloud.api.component.group.IServerGroup;
import lombok.Getter;

import java.util.Collection;

@Getter
public final class ServerGroupUpdatedEvent extends ServerGroupEvent {

    private final Collection<String> oldTemplates;
    private final Collection<String> newTemplates;

    public ServerGroupUpdatedEvent(IServerGroup serverGroup, Collection<String> oldTemplates, Collection<String> newTemplates) {
        super(serverGroup);
        this.oldTemplates = oldTemplates;
        this.newTemplates = newTemplates;
    }
}