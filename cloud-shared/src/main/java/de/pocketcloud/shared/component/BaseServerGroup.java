package de.pocketcloud.shared.component;

import de.pocketcloud.api.component.group.IServerGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;

@Getter
@Accessors(fluent = true, chain = false)
@AllArgsConstructor
public class BaseServerGroup implements IServerGroup {

    protected final String name;
    @Setter
    protected Collection<String> templates;

    public Collection<String> templates() {
        return templates.stream().toList();
    }
}