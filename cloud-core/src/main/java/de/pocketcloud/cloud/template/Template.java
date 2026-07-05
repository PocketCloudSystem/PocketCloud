package de.pocketcloud.cloud.template;

import com.google.gson.annotations.JsonAdapter;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.player.CloudPlayerManager;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.template.group.ServerGroupManager;
import de.pocketcloud.cloud.template.util.conv.ServerSoftwareConverter;
import de.pocketcloud.cloud.template.util.conv.TemplateTypeConverter;
import de.pocketcloud.cloud.util.FilterableObject;
import de.pocketcloud.common.mapper.MapInline;
import de.pocketcloud.common.mapper.MapKey;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.serialization.Writable;
import de.pocketcloud.cloud.util.gson.TemplateJsonSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@JsonAdapter(TemplateJsonSerializer.class)
@Accessors(fluent = true)
public record Template(String name, @MapInline TemplateSettings settings,
                       @MapKey(converter = TemplateTypeConverter.class) TemplateType templateType,
                       @MapKey(converter = ServerSoftwareConverter.class) ServerSoftware serverSoftware) implements Writable<Map<String, Object>>, FilterableObject {

    public Template setLobby(boolean lobby) {
        settings.setLobby(lobby);
        return this;
    }

    public Template setMaintenance(boolean maintenance) {
        settings.setMaintenance(maintenance);
        return this;
    }

    public Template setStaticServers(boolean staticServers) {
        settings.setStaticServers(staticServers);
        return this;
    }

    public Template setAlwaysCopyToStaticServers(boolean alwaysCopyToStaticServers) {
        settings.setAlwaysCopyToStaticServers(alwaysCopyToStaticServers);
        return this;
    }

    public Template setMaxPlayerCount(int maxPlayerCount) {
        settings.setMaxPlayerCount(maxPlayerCount);
        return this;
    }

    public Template setMinServerCount(int minServerCount) {
        settings.setMinServerCount(minServerCount);
        return this;
    }

    public Template setMaxServerCount(int maxServerCount) {
        settings.setMaxServerCount(maxServerCount);
        return this;
    }

    public Template setStartNewPercentage(double startNewPercentage) {
        settings.setStartNewPercentage(startNewPercentage);
        return this;
    }

    public Template setAutoStart(boolean autoStart) {
        settings.setAutoStart(autoStart);
        return this;
    }

    public boolean isParentGroup(ServerGroup serverGroup) {
        return ServerGroupManager.instance().get(this).contains(serverGroup);
    }

    public boolean isParentGroup(String serverGroup) {
        return ServerGroupManager.instance().get(this).stream().anyMatch(g -> g.name().equals(serverGroup));
    }

    public boolean isTypeOf(TemplateType type) {
        return this.templateType.equals(type);
    }

    public boolean isCompatibleWith(ServerSoftware software) {
        return this.serverSoftware.name().equals(software.name());
    }

    public List<CloudPlayer> players() {
        return CloudPlayerManager.instance().getAll(this);
    }

    public long playerCount() {
        return players().size();
    }

    public List<ServerGroup> parentGroups() {
        return ServerGroupManager.instance().get(this);
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

    @Getter
    @Setter
    @Accessors(fluent = false)
    public static final class TemplateSettings implements Writable<Map<String, Object>> {

        private boolean lobby;
        private boolean maintenance;
        private boolean staticServers;
        private boolean alwaysCopyToStaticServers;
        private boolean saveOnShutdown;
        private int maxPlayerCount;
        private int minServerCount;
        private int maxServerCount;
        private double startNewPercentage;
        private boolean autoStart;

        public TemplateSettings(boolean lobby, boolean maintenance, boolean staticServers, boolean alwaysCopyToStaticServers, boolean saveOnShutdown, int maxPlayerCount, int minServerCount, int maxServerCount, double startNewPercentage, boolean autoStart) {
            this.lobby = lobby;
            this.maintenance = maintenance;
            this.staticServers = staticServers;
            this.alwaysCopyToStaticServers = alwaysCopyToStaticServers;
            this.saveOnShutdown = saveOnShutdown;
            this.maxPlayerCount = maxPlayerCount;
            this.minServerCount = minServerCount;
            this.maxServerCount = maxServerCount;
            this.startNewPercentage = startNewPercentage;
            this.autoStart = autoStart;
        }

        @Override
        public Map<String, Object> write() {
            return MapperUtils.toMap(this);
        }

        public static TemplateSettings read(Map<String, Object> map) {
            return MapperUtils.fromMap(map, TemplateSettings.class);
        }
    }
}