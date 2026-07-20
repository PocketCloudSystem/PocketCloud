package de.pocketcloud.cloud.builder;

import de.pocketcloud.api.component.builder.IServerGroupBuilder;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.template.group.ServerGroup;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

@Accessors(fluent = true)
public final class ServerGroupBuilder implements IServerGroupBuilder {

    @Setter
    private String name;
    private final Collection<String> templates = new HashSet<>();

    @Override
    public IServerGroupBuilder template(String templateName) {
        templates.add(templateName);
        return this;
    }

    @Override
    public IServerGroupBuilder template(ITemplate template) {
        templates.add(template.name());
        return this;
    }

    @Override
    public IServerGroupBuilder templates(String... templateNames) {
        templates.addAll(Arrays.stream(templateNames).toList());
        return this;
    }

    @Override
    public IServerGroupBuilder templates(ITemplate... templates) {
        this.templates.addAll(Arrays.stream(templates).map(ITemplate::name).toList());
        return this;
    }

    @Override
    public IServerGroupBuilder clearTemplates() {
        templates.clear();
        return this;
    }

    @Override
    public IServerGroup build() {
        return new ServerGroup(name, templates);
    }
}