package de.pocketcloud.api.component.template;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.search.ServerGroupSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.settings.TemplateSettings;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.common.serialization.Writable;

import java.util.Collection;
import java.util.Map;

public interface ITemplate extends Writable<Map<String, Object>> {

    default boolean isParentGroup(IServerGroup serverGroup) {
        return isParentGroup(serverGroup.name());
    }

    default boolean isParentGroup(String name) {
        return !CloudAPI.instance().serverGroups().query(ServerGroupSearchQuery.create()
                .nameStartsWith(name)
                .withTemplates(this)).isEmpty();
    }

    default boolean isTypeOf(TemplateType type) {
        return templateType().equals(type);
    }

    default boolean isCompatibleWith(IServerSoftware software) {
        return serverSoftware().name().equals(software.name());
    }

    default Collection<ICloudPlayer> players() {
        return CloudAPI.instance().players().query(q -> q.ofTemplate(this));
    }

    default long playerCount() {
        return players().size();
    }

    String name();

    TemplateSettings settings();

    TemplateType templateType();

    IServerSoftware serverSoftware();

    default Collection<IServerGroup> parentGroups() {
        return CloudAPI.instance().serverGroups().query(q -> q.withTemplates(this));
    }

    default Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }
}