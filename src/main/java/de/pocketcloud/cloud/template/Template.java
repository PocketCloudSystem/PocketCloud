package de.pocketcloud.cloud.template;

import com.google.gson.annotations.JsonAdapter;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.template.group.ServerGroupManager;
import de.pocketcloud.cloud.template.util.conv.ServerSoftwareConverter;
import de.pocketcloud.cloud.template.util.conv.TemplateTypeConverter;
import de.pocketcloud.cloud.util.mapper.MapInline;
import de.pocketcloud.cloud.util.mapper.MapKey;
import de.pocketcloud.cloud.util.mapper.MapperUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.cloud.util.Writable;
import de.pocketcloud.cloud.util.gson.TemplateJsonSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@JsonAdapter(TemplateJsonSerializer.class)
@Getter
@Accessors(fluent = true)
@NoArgsConstructor
public final class Template implements Writable<Map<String, Object>> {

    private String name;
    @MapInline
    private TemplateSettings settings;
    @MapKey(converter = TemplateTypeConverter.class)
    private TemplateType templateType;
    @MapKey(converter = ServerSoftwareConverter.class)
    private ServerSoftware serverSoftware;

    public Template(String name, TemplateSettings settings, TemplateType templateType, ServerSoftware serverSoftware) {
        this.name = name;
        this.settings = settings;
        this.templateType = templateType;
        this.serverSoftware = serverSoftware;
    }

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

    public Template setStartNewPercentage(float startNewPercentage) {
        settings.setStartNewPercentage(startNewPercentage);
        return this;
    }

    public Template setAutoStart(boolean autoStart) {
        settings.setAutoStart(autoStart);
        return this;
    }

    public boolean isParentGroup(ServerGroup serverGroup) {
        return ServerGroupManager.getInstance().get(this).contains(serverGroup);
    }

    public boolean isTypeOf(TemplateType type) {
        return this.templateType.equals(type);
    }

    public boolean isCompatibleWith(ServerSoftware software) {
        return this.serverSoftware.name().equals(software.name());
    }

    public List<ServerGroup> parentGroups() {
        return ServerGroupManager.getInstance().get(this);
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
        private int maxPlayerCount;
        private int minServerCount;
        private int maxServerCount;
        private float startNewPercentage;
        private boolean autoStart;

        public TemplateSettings(boolean lobby, boolean maintenance, boolean staticServers, boolean alwaysCopyToStaticServers, int maxPlayerCount, int minServerCount, int maxServerCount, float startNewPercentage, boolean autoStart) {
            this.lobby = lobby;
            this.maintenance = maintenance;
            this.staticServers = staticServers;
            this.alwaysCopyToStaticServers = alwaysCopyToStaticServers;
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