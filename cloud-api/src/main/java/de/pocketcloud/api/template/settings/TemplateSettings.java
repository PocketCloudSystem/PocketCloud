package de.pocketcloud.api.template.settings;

import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.common.serialization.Writable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

@Getter
@Setter
@Accessors(fluent = true)
public final class TemplateSettings implements Writable<Map<String, Object>> {

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