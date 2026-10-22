package de.pocketcloud.shared.component;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Accessors(fluent = true, chain = false)
public class BaseServerGroup implements IServerGroup {

    protected final String name;
    @Setter
    protected Collection<String> templates;

    @MapCreator
    public BaseServerGroup(
            @MapKey(name = "name") String name,
            @MapKey(name = "templates") Collection<String> templates
    ) {
        this.name = name;
        this.templates = templates != null ? templates : new ArrayList<>();
    }

    public Collection<String> templates() {
        return List.copyOf(templates);
    }
}