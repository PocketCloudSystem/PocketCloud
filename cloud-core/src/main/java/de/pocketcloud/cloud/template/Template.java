package de.pocketcloud.cloud.template;

import com.google.gson.annotations.JsonAdapter;
import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.search.PlayerSearchQuery;
import de.pocketcloud.api.search.ServerGroupSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.settings.TemplateSettings;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.template.util.conv.ServerSoftwareConverter;
import de.pocketcloud.cloud.template.util.conv.TemplateTypeConverter;
import de.pocketcloud.common.mapper.MapInline;
import de.pocketcloud.common.mapper.MapKey;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.serialization.Writable;
import de.pocketcloud.cloud.util.gson.TemplateJsonSerializer;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

@JsonAdapter(TemplateJsonSerializer.class)
@Accessors(fluent = true)
public record Template(String name, @MapInline TemplateSettings settings,
                       @MapKey(converter = TemplateTypeConverter.class) TemplateType templateType,
                       @MapKey(converter = ServerSoftwareConverter.class) ServerSoftware serverSoftware) implements Writable<Map<String, Object>>, ITemplate {

    public boolean isParentGroup(ServerGroup serverGroup) {
        return isParentGroup(serverGroup.name());
    }

    public boolean isParentGroup(String serverGroupName) {
        return !PocketCloud.instance().serverGroups().query(ServerGroupSearchQuery.create()
                .nameStartsWith(serverGroupName)
                .withTemplates(this)).isEmpty();
    }

    public boolean isTypeOf(TemplateType type) {
        return this.templateType.equals(type);
    }

    public boolean isCompatibleWith(ServerSoftware software) {
        return this.serverSoftware.name().equals(software.name());
    }

    public Collection<CloudPlayer> players() {
        return PocketCloud.instance().players().query(PlayerSearchQuery.create().ofTemplate(this));
    }

    public long playerCount() {
        return players().size();
    }

    public Collection<ServerGroup> parentGroups() {
        return PocketCloud.instance().serverGroups().query(ServerGroupSearchQuery.create().withTemplates(this));
    }

    public Path path() {
        return PocketCloudPaths.templates().with(name).asPath();
    }

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static Template read(Map<String, Object> map) {
        return MapperUtils.fromMap(map, Template.class);
    }
}