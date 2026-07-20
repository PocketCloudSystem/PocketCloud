package de.pocketcloud.api.provider.write;

import de.pocketcloud.api.component.builder.IServerGroupBuilder;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.provider.IServerGroupProvider;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface IWriteServerGroupProvider extends IServerGroupProvider {

    void create(IServerGroupBuilder builder);

    void add(IServerGroup serverGroup);

    void addTemplate(IServerGroup serverGroup, ITemplate template);

    void removeTemplate(IServerGroup serverGroup, ITemplate template);

    void remove(IServerGroup serverGroup);

    void delete(IServerGroup serverGroup);
}