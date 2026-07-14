package de.pocketcloud.cloud.template.group;

import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.serialization.Writable;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Accessors(fluent = true)
public record ServerGroup(String name,
                          List<String> templates) implements Writable<Map<String, Object>>, IServerGroup {

    public ServerGroup add(Template template) {
        return add(template.name());
    }

    public ServerGroup add(String template) {
        if (!templates.contains(template)) templates.add(template);
        return this;
    }

    public ServerGroup remove(Template template) {
        return remove(template.name());
    }

    public ServerGroup remove(String template) {
        templates.remove(template);
        return this;
    }

    public boolean is(Template template) {
        return is(template.name());
    }

    public boolean is(String template) {
        return templates.contains(template);
    }

    public Path path() {
        return PocketCloudPaths.groups().with(name).asPath();
    }

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static ServerGroup read(Map<String, Object> map) {
        return MapperUtils.fromMap(map, ServerGroup.class);
    }
}