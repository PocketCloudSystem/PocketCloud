package de.pocketcloud.api.template.util;

import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.template.settings.TemplateSettings;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.serialization.Writable;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

@Setter
@Accessors(fluent = true)
public class TemplateEditData implements Writable<Map<String, Object>> {

    private Boolean lobby = null;
    private Boolean maintenance = null;
    private Boolean staticServers = null;
    private Boolean alwaysCopyToStaticServers = null;
    private Boolean saveOnShutdown = null;
    private Boolean deleteOnStop = null;
    private Boolean stopOnEmpty = null;
    private Boolean autoStart = null;

    private Integer maxPlayerCount = null;
    private Integer minServerCount = null;
    private Integer maxServerCount = null;
    private Double startNewServerThreshold = null;
    private Integer maxMemory = null;

    private Integer startupTimeout = null;
    private Integer shutdownTimeout = null;
    private Integer emptyServerGracePeriod = null;
    private Integer priority = null;

    /**
     * {@link TemplateEditData#create()}
     */
    private TemplateEditData() {}

    public void applyTo(ITemplate template) {
        if (maxPlayerCount != null && maxPlayerCount < 1)
            throw new IllegalArgumentException("Max player count must be at least 1");
        if (minServerCount != null && minServerCount < 0)
            throw new IllegalArgumentException("Min server count must be >= 0");
        if (maxServerCount != null && maxServerCount < 1)
            throw new IllegalArgumentException("Max server count must be at least 1");
        if (startNewServerThreshold != null && (startNewServerThreshold < 0 || startNewServerThreshold > 1))
            throw new IllegalArgumentException("Start new server threshold must be between 0 and 1");
        if (maxMemory != null && maxMemory <= 0)
            throw new IllegalArgumentException("Max memory must be positive");
        if (startupTimeout != null && startupTimeout < 0)
            throw new IllegalArgumentException("Startup timeout must be >= 0");
        if (shutdownTimeout != null && shutdownTimeout < 0)
            throw new IllegalArgumentException("Shutdown timeout must be >= 0");
        if (emptyServerGracePeriod != null && emptyServerGracePeriod < 0)
            throw new IllegalArgumentException("Empty server grace period must be >= 0");
        if (priority != null && priority < 0)
            throw new IllegalArgumentException("Priority must be >= 0");

        TemplateSettings settings = template.settings();

        if (lobby != null) settings.lobby(lobby);
        if (maintenance != null) settings.maintenance(maintenance);
        if (staticServers != null) settings.staticServers(staticServers);
        if (alwaysCopyToStaticServers != null) settings.alwaysCopyToStaticServers(alwaysCopyToStaticServers);
        if (saveOnShutdown != null) settings.saveOnShutdown(saveOnShutdown);
        if (deleteOnStop != null) settings.deleteOnStop(deleteOnStop);
        if (stopOnEmpty != null) settings.stopOnEmpty(stopOnEmpty);
        if (autoStart != null) settings.autoStart(autoStart);

        if (maxPlayerCount != null) settings.maxPlayerCount(maxPlayerCount);
        if (minServerCount != null) settings.minServerCount(minServerCount);
        if (maxServerCount != null) settings.maxServerCount(maxServerCount);
        if (startNewServerThreshold != null) settings.startNewServerThreshold(startNewServerThreshold);
        if (maxMemory != null) settings.maxMemory(maxMemory);

        if (startupTimeout != null) settings.startupTimeout(startupTimeout);
        if (shutdownTimeout != null) settings.shutdownTimeout(shutdownTimeout);
        if (emptyServerGracePeriod != null) settings.emptyServerGracePeriod(emptyServerGracePeriod);
        if (priority != null) settings.priority(priority);
    }

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static TemplateEditData create() {
        return new TemplateEditData();
    }

    public static TemplateEditData between(TemplateSettings oldSettings, TemplateSettings newSettings) {
        TemplateEditData data = new TemplateEditData();

        if (oldSettings.lobby() != newSettings.lobby())
            data.lobby = newSettings.lobby();
        if (oldSettings.maintenance() != newSettings.maintenance())
            data.maintenance = newSettings.maintenance();
        if (oldSettings.staticServers() != newSettings.staticServers())
            data.staticServers = newSettings.staticServers();
        if (oldSettings.alwaysCopyToStaticServers() != newSettings.alwaysCopyToStaticServers())
            data.alwaysCopyToStaticServers = newSettings.alwaysCopyToStaticServers();
        if (oldSettings.saveOnShutdown() != newSettings.saveOnShutdown())
            data.saveOnShutdown = newSettings.saveOnShutdown();
        if (oldSettings.deleteOnStop() != newSettings.deleteOnStop())
            data.deleteOnStop = newSettings.deleteOnStop();
        if (oldSettings.stopOnEmpty() != newSettings.stopOnEmpty())
            data.stopOnEmpty = newSettings.stopOnEmpty();
        if (oldSettings.autoStart() != newSettings.autoStart())
            data.autoStart = newSettings.autoStart();

        if (oldSettings.maxPlayerCount() != newSettings.maxPlayerCount())
            data.maxPlayerCount = newSettings.maxPlayerCount();
        if (oldSettings.minServerCount() != newSettings.minServerCount())
            data.minServerCount = newSettings.minServerCount();
        if (oldSettings.maxServerCount() != newSettings.maxServerCount())
            data.maxServerCount = newSettings.maxServerCount();
        if (oldSettings.startNewServerThreshold() != newSettings.startNewServerThreshold())
            data.startNewServerThreshold = newSettings.startNewServerThreshold();
        if (oldSettings.maxMemory() != newSettings.maxMemory())
            data.maxMemory = newSettings.maxMemory();

        if (oldSettings.startupTimeout() != newSettings.startupTimeout())
            data.startupTimeout = newSettings.startupTimeout();
        if (oldSettings.shutdownTimeout() != newSettings.shutdownTimeout())
            data.shutdownTimeout = newSettings.shutdownTimeout();
        if (oldSettings.emptyServerGracePeriod() != newSettings.emptyServerGracePeriod())
            data.emptyServerGracePeriod = newSettings.emptyServerGracePeriod();
        if (oldSettings.priority() != newSettings.priority())
            data.priority = newSettings.priority();

        return data;
    }

    public static TemplateEditData read(Map<String, Object> data) {
        TemplateEditData editData = new TemplateEditData();

        editData.lobby = (Boolean) data.get("lobby");
        editData.maintenance = (Boolean) data.get("maintenance");
        editData.staticServers = (Boolean) data.get("staticServers");
        editData.alwaysCopyToStaticServers = (Boolean) data.get("alwaysCopyToStaticServers");
        editData.saveOnShutdown = (Boolean) data.get("saveOnShutdown");
        editData.deleteOnStop = (Boolean) data.get("deleteOnStop");
        editData.stopOnEmpty = (Boolean) data.get("stopOnEmpty");
        editData.autoStart = (Boolean) data.get("autoStart");

        editData.maxPlayerCount = data.get("maxPlayerCount") != null
                ? ((Number) data.get("maxPlayerCount")).intValue() : null;
        editData.minServerCount = data.get("minServerCount") != null
                ? ((Number) data.get("minServerCount")).intValue() : null;
        editData.maxServerCount = data.get("maxServerCount") != null
                ? ((Number) data.get("maxServerCount")).intValue() : null;
        editData.startNewServerThreshold = data.get("startNewServerThreshold") != null
                ? ((Number) data.get("startNewServerThreshold")).doubleValue() : null;
        editData.maxMemory = data.get("maxMemory") != null
                ? ((Number) data.get("maxMemory")).intValue() : null;

        editData.startupTimeout = data.get("startupTimeout") != null
                ? ((Number) data.get("startupTimeout")).intValue() : null;
        editData.shutdownTimeout = data.get("shutdownTimeout") != null
                ? ((Number) data.get("shutdownTimeout")).intValue() : null;
        editData.emptyServerGracePeriod = data.get("emptyServerGracePeriod") != null
                ? ((Number) data.get("emptyServerGracePeriod")).intValue() : null;
        editData.priority = data.get("priority") != null
                ? ((Number) data.get("priority")).intValue() : null;

        return editData;
    }
}