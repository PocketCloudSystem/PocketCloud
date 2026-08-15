package de.pocketcloud.shared.component;

import de.pocketcloud.api.component.group.IServerGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.List;

@Getter
@Accessors(fluent = true, chain = false)
public class BaseServerGroup implements IServerGroup {

    protected final String name;
    @Setter
    protected Collection<String> templates;

    public BaseServerGroup(String name, Collection<String> templates) {
        this.name = name;
        this.templates = templates;
    }

    public Collection<String> templates() {
        return List.copyOf(templates);
    }
}