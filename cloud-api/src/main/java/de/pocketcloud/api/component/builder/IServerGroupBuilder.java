package de.pocketcloud.api.component.builder;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;

public interface IServerGroupBuilder extends IComponentBuilder<IServerGroup> {

    IServerGroupBuilder name(String name);

    IServerGroupBuilder template(String templateName);

    IServerGroupBuilder template(ITemplate template);

    IServerGroupBuilder templates(String... templateNames);

    IServerGroupBuilder templates(ITemplate... templates);

    IServerGroupBuilder clearTemplates();
}