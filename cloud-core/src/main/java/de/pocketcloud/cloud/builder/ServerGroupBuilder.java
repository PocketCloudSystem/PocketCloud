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

    public static ServerGroupBuilder create() {
        return new ServerGroupBuilder();
    }
    
    public static ServerGroupBuilder of(IServerGroup group) {
        return ServerGroupBuilder.create()
                .name(group.name())
                .templates(group.templates().toArray(new String[0]));
    }

    @Override
    public ServerGroupBuilder template(String templateName) {
        templates.add(templateName);
        return this;
    }

    @Override
    public ServerGroupBuilder template(ITemplate template) {
        templates.add(template.name());
        return this;
    }

    @Override
    public ServerGroupBuilder templates(String... templateNames) {
        templates.addAll(Arrays.stream(templateNames).toList());
        return this;
    }

    @Override
    public ServerGroupBuilder templates(ITemplate... templates) {
        this.templates.addAll(Arrays.stream(templates).map(ITemplate::name).toList());
        return this;
    }

    @Override
    public ServerGroupBuilder clearTemplates() {
        templates.clear();
        return this;
    }

    @Override
    public IServerGroup build() {
        return new ServerGroup(name, templates);
    }
}