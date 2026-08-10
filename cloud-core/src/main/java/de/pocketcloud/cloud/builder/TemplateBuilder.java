package de.pocketcloud.cloud.builder;

import de.pocketcloud.api.component.builder.ITemplateBuilder;
import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.settings.TemplateSettings;
import de.pocketcloud.cloud.template.Template;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true)
public final class TemplateBuilder implements ITemplateBuilder {

    private String name;
    private boolean lobby = false;
    private boolean maintenance = true;
    private boolean staticServers = false;
    private boolean alwaysCopyToStaticServers = true;
    private boolean saveOnShutdown = false;
    private int maxPlayerCount = 20;
    private int minServerCount = 1;
    private int maxServerCount = 2;
    private double startNewPercentage = 0;
    private boolean autoStart = false;
    private int maxMemory = 1024;
    private TemplateType type = TemplateType.SERVER;
    private IServerSoftware software;

    public static TemplateBuilder create() {
        return new TemplateBuilder();
    }

    @Override
    public ITemplate build() {
        if (name == null) throw new NullPointerException("Template name is null");
        if (maxPlayerCount < 0) throw new IllegalArgumentException("Max player count must be positive");
        if (minServerCount < 0) throw new IllegalArgumentException("Min server count must be positive");
        if (maxServerCount < 0) throw new IllegalArgumentException("Max server count must be positive");
        if (startNewPercentage < 0 || startNewPercentage > 1)
            throw new IllegalArgumentException("Start new percentage must be between 0 and 1");
        if (maxMemory <= 0) throw new IllegalArgumentException("Max memory must be positive");
        if (software == null) throw new NullPointerException("Template software is null");
        return new Template(
                name,
                new TemplateSettings(
                        lobby,
                        maintenance,
                        staticServers,
                        alwaysCopyToStaticServers,
                        saveOnShutdown,
                        maxPlayerCount,
                        minServerCount,
                        maxServerCount,
                        startNewPercentage,
                        autoStart,
                        maxMemory
                ),
                type,
                software
        );
    }
}