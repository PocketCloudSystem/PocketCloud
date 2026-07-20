package de.pocketcloud.api.component.group;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.common.serialization.Writable;

import java.util.Collection;
import java.util.Map;

public interface IServerGroup extends Writable<Map<String, Object>> {

    default boolean is(ITemplate template) {
        return is(template.name());
    }

    default boolean is(String template) {
        return templates().contains(template);
    }

    String name();

    default Collection<ICloudServer> servers() {
        return CloudAPI.instance().servers().query(q -> q.inGroup(this));
    }

    default Collection<ICloudPlayer> players() {
        return CloudAPI.instance().players().query(q -> q.inGroup(this));
    }

    default int playerCount() {
        return players().size();
    }

    Collection<String> templates();

    void templates(Collection<String> templates);

    default Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }
}