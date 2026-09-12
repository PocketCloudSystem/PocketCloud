package de.pocketcloud.api.template.settings;

import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.serialization.Writable;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@Accessors(fluent = true)
public final class TemplateSettings implements Writable<Map<String, Object>> {

    @ApiStatus.Internal
    private transient boolean usingDefaults;

    private boolean lobby;
    private boolean maintenance;
    private boolean staticServers;
    private boolean alwaysCopyToStaticServers;
    private boolean saveOnShutdown;
    private boolean deleteOnStop;
    private boolean stopOnEmpty;
    private boolean autoStart;

    private double startNewServerThreshold;
    private int maxPlayerCount;
    private int minServerCount;
    private int maxServerCount;

    private int maxMemory;

    private int startupTimeout;
    private int shutdownTimeout;
    private int emptyServerGracePeriod;

    private int priority;

    private List<String> jvmFlags;

    @MapCreator
    public TemplateSettings(
            @MapKey(name = "lobby") Boolean lobby,
            @MapKey(name = "maintenance") Boolean maintenance,
            @MapKey(name = "staticServers") Boolean staticServers,
            @MapKey(name = "alwaysCopyToStaticServers") Boolean alwaysCopyToStaticServers,
            @MapKey(name = "saveOnShutdown") Boolean saveOnShutdown,
            @MapKey(name = "deleteOnStop") Boolean deleteOnStop,
            @MapKey(name = "stopOnEmpty") Boolean stopOnEmpty,
            @MapKey(name = "autoStart") Boolean autoStart,
            @MapKey(name = "startNewServerThreshold") Double startNewServerThreshold,
            @MapKey(name = "maxPlayerCount") Integer maxPlayerCount,
            @MapKey(name = "minServerCount") Integer minServerCount,
            @MapKey(name = "maxServerCount") Integer maxServerCount,
            @MapKey(name = "maxMemory") Integer maxMemory,
            @MapKey(name = "startupTimeout") Integer startupTimeout,
            @MapKey(name = "shutdownTimeout") Integer shutdownTimeout,
            @MapKey(name = "emptyServerGracePeriod") Integer emptyServerGracePeriod,
            @MapKey(name = "priority") Integer priority,
            @MapKey(name = "jvmFlags") List<String> jvmFlags
    ) {
        boolean anyNull = Objects.isNull(lobby)
                        || Objects.isNull(maintenance)
                        || Objects.isNull(staticServers)
                        || Objects.isNull(alwaysCopyToStaticServers)
                        || Objects.isNull(saveOnShutdown)
                        || Objects.isNull(deleteOnStop)
                        || Objects.isNull(stopOnEmpty)
                        || Objects.isNull(autoStart)
                        || Objects.isNull(startNewServerThreshold)
                        || Objects.isNull(maxPlayerCount)
                        || Objects.isNull(minServerCount)
                        || Objects.isNull(maxServerCount)
                        || Objects.isNull(maxMemory)
                        || Objects.isNull(startupTimeout)
                        || Objects.isNull(shutdownTimeout)
                        || Objects.isNull(emptyServerGracePeriod)
                        || Objects.isNull(priority)
                        || Objects.isNull(jvmFlags);

        if (anyNull) usingDefaults = true;

        this.lobby = Objects.requireNonNullElse(lobby, false);
        this.maintenance = Objects.requireNonNullElse(maintenance, false);
        this.staticServers = Objects.requireNonNullElse(staticServers, false);
        this.alwaysCopyToStaticServers = Objects.requireNonNullElse(alwaysCopyToStaticServers, false);
        this.saveOnShutdown = Objects.requireNonNullElse(saveOnShutdown, true);
        this.deleteOnStop = Objects.requireNonNullElse(deleteOnStop, true);
        this.stopOnEmpty = Objects.requireNonNullElse(stopOnEmpty, false);
        this.autoStart = Objects.requireNonNullElse(autoStart, false);

        this.startNewServerThreshold = Objects.requireNonNullElse(startNewServerThreshold, 0d);
        this.maxPlayerCount = Objects.requireNonNullElse(maxPlayerCount, 20);
        this.minServerCount = Objects.requireNonNullElse(minServerCount, 0);
        this.maxServerCount = Objects.requireNonNullElse(maxServerCount, 2);

        this.maxMemory = Objects.requireNonNullElse(maxMemory, 1024);

        this.startupTimeout = Objects.requireNonNullElse(startupTimeout, 15);
        this.shutdownTimeout = Objects.requireNonNullElse(shutdownTimeout, 10);
        this.emptyServerGracePeriod = Objects.requireNonNullElse(emptyServerGracePeriod, 0);

        this.priority = Objects.requireNonNullElse(priority, 0);

        this.jvmFlags = Objects.requireNonNullElse(jvmFlags, List.of());
    }

    public void applyFrom(TemplateSettings templateSettings) {
        this.lobby = templateSettings.lobby;
        this.maintenance = templateSettings.maintenance;
        this.staticServers = templateSettings.staticServers;
        this.alwaysCopyToStaticServers = templateSettings.alwaysCopyToStaticServers;
        this.saveOnShutdown = templateSettings.saveOnShutdown;
        this.deleteOnStop = templateSettings.deleteOnStop;
        this.stopOnEmpty = templateSettings.stopOnEmpty;
        this.autoStart = templateSettings.autoStart;

        this.startNewServerThreshold = templateSettings.startNewServerThreshold;
        this.maxPlayerCount = templateSettings.maxPlayerCount;
        this.minServerCount = templateSettings.minServerCount;
        this.maxServerCount = templateSettings.maxServerCount;

        this.maxMemory = templateSettings.maxMemory;

        this.startupTimeout = templateSettings.startupTimeout;
        this.shutdownTimeout = templateSettings.shutdownTimeout;
        this.emptyServerGracePeriod = templateSettings.emptyServerGracePeriod;

        this.priority = templateSettings.priority;

        this.jvmFlags = templateSettings.jvmFlags;
    }

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static TemplateSettings read(Map<String, Object> map) {
        return MapperUtils.fromMap(map, TemplateSettings.class);
    }
}