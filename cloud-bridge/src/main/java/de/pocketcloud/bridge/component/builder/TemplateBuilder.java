package de.pocketcloud.bridge.component.builder;

import de.pocketcloud.api.component.builder.ITemplateBuilder;
import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.settings.TemplateSettings;
import de.pocketcloud.bridge.component.Template;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Setter
@Accessors(fluent = true)
public final class TemplateBuilder implements ITemplateBuilder {

    private String name;

    private boolean lobby = false;
    private boolean maintenance = true;
    private boolean staticServers = false;
    private boolean alwaysCopyToStaticServers = true;
    private boolean saveOnShutdown = false;
    private boolean deleteOnStop = false;
    private boolean stopOnEmpty = false;
    private boolean autoStart = false;

    private double startNewServerThreshold = 0;
    private int maxPlayerCount = 20;
    private int minServerCount = 1;
    private int maxServerCount = 2;

    private int maxMemory = 1024;

    private int startupTimeout = 60;
    private int shutdownTimeout = 30;
    private int emptyServerGracePeriod = 60;

    private int priority = 0;

    private List<String> jvmFlags = new ArrayList<>();

    private TemplateType type = TemplateType.SERVER;
    private IServerSoftware software;

    public static TemplateBuilder create() {
        return new TemplateBuilder();
    }

    public static TemplateBuilder of(ITemplate template) {
        TemplateSettings settings = template.settings();

        return TemplateBuilder.create()
                .name(template.name())
                .lobby(settings.lobby())
                .maintenance(settings.maintenance())
                .staticServers(settings.staticServers())
                .alwaysCopyToStaticServers(settings.alwaysCopyToStaticServers())
                .saveOnShutdown(settings.saveOnShutdown())
                .deleteOnStop(settings.deleteOnStop())
                .stopOnEmpty(settings.stopOnEmpty())
                .autoStart(settings.autoStart())
                .startNewServerThreshold(settings.startNewServerThreshold())
                .maxPlayerCount(settings.maxPlayerCount())
                .minServerCount(settings.minServerCount())
                .maxServerCount(settings.maxServerCount())
                .maxMemory(settings.maxMemory())
                .startupTimeout(settings.startupTimeout())
                .shutdownTimeout(settings.shutdownTimeout())
                .emptyServerGracePeriod(settings.emptyServerGracePeriod())
                .priority(settings.priority())
                .jvmFlags(new ArrayList<>(settings.jvmFlags()))
                .type(template.templateType())
                .software(template.serverSoftware());
    }

    @Override
    public ITemplate build() {
        if (name == null) throw new NullPointerException("Template name is null");
        if (maxPlayerCount < 0) throw new IllegalArgumentException("Max player count must be positive");
        if (minServerCount < 0) throw new IllegalArgumentException("Min server count must be positive");
        if (maxServerCount < 0) throw new IllegalArgumentException("Max server count must be positive");
        if (minServerCount > maxServerCount) throw new IllegalArgumentException("Min server count cannot be greater than max server count");
        if (startNewServerThreshold < 0 || startNewServerThreshold > 1) throw new IllegalArgumentException("Start new server threshold must be between 0 and 1");
        if (maxMemory <= 0) throw new IllegalArgumentException("Max memory must be positive");
        if (startupTimeout < 0) throw new IllegalArgumentException("Startup timeout must be positive");
        if (shutdownTimeout < 0) throw new IllegalArgumentException("Shutdown timeout must be positive");
        if (emptyServerGracePeriod < 0) throw new IllegalArgumentException("Empty server grace period must be positive");
        if (software == null) throw new NullPointerException("Template software is null");
        return new Template(
                name,
                new TemplateSettings(
                        lobby,
                        maintenance,
                        staticServers,
                        alwaysCopyToStaticServers,
                        saveOnShutdown,
                        deleteOnStop,
                        stopOnEmpty,
                        autoStart,
                        startNewServerThreshold,
                        maxPlayerCount,
                        minServerCount,
                        maxServerCount,
                        maxMemory,
                        startupTimeout,
                        shutdownTimeout,
                        emptyServerGracePeriod,
                        priority,
                        jvmFlags
                ),
                type,
                software
        );
    }
}