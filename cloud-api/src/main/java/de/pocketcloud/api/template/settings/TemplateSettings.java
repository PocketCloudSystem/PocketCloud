package de.pocketcloud.api.template.settings;

import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.serialization.Writable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

@Getter
@Setter
@Accessors(fluent = true)
@AllArgsConstructor
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
    private int maxMemory;

    public void applyFrom(TemplateSettings templateSettings) {
        this.lobby = templateSettings.lobby;
        this.maintenance = templateSettings.maintenance;
        this.staticServers = templateSettings.staticServers;
        this.alwaysCopyToStaticServers = templateSettings.alwaysCopyToStaticServers;
        this.saveOnShutdown = templateSettings.saveOnShutdown;
        this.maxPlayerCount = templateSettings.maxPlayerCount;
        this.minServerCount = templateSettings.minServerCount;
        this.maxServerCount = templateSettings.maxServerCount;
        this.startNewPercentage = templateSettings.startNewPercentage;
        this.autoStart = templateSettings.autoStart;
        this.maxMemory = templateSettings.maxMemory;
    }

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static TemplateSettings read(Map<String, Object> map) {
        return MapperUtils.fromMap(map, TemplateSettings.class);
    }
}