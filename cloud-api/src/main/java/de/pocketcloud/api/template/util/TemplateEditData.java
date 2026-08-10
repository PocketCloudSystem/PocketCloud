package de.pocketcloud.api.template.util;

import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.template.settings.TemplateSettings;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true)
public class TemplateEditData {

    private Boolean lobby = null;
    private Boolean maintenance = null;
    private Boolean staticServers = null;
    private Boolean alwaysCopyToStaticServers = null;
    private Boolean saveOnShutdown = null;
    private Integer maxPlayerCount = null;
    private Integer minServerCount = null;
    private Integer maxServerCount = null;
    private Double startNewPercentage = null;
    private Boolean autoStart = null;
    private Integer maxMemory = null;

    /**
     * {@link TemplateEditData#create()}
     */
    private TemplateEditData() {}

    public void applyTo(ITemplate template) {
        if (maxPlayerCount != null && maxPlayerCount < 0)
            throw new IllegalArgumentException("Max player count must be positive");
        if (minServerCount != null && minServerCount < 0)
            throw new IllegalArgumentException("Min server count must be positive");
        if (maxServerCount != null && maxServerCount < 0)
            throw new IllegalArgumentException("Max server count must be positive");
        if (startNewPercentage != null && (startNewPercentage < 0 || startNewPercentage > 1))
            throw new IllegalArgumentException("Start new percentage must be between 0 and 1");
        if (maxMemory <= 0) throw new IllegalArgumentException("Max memory must be positive");

        if (lobby != null) template.settings().lobby(lobby);
        if (maintenance != null) template.settings().maintenance(maintenance);
        if (staticServers != null) template.settings().staticServers(staticServers);
        if (alwaysCopyToStaticServers != null) template.settings().alwaysCopyToStaticServers(alwaysCopyToStaticServers);
        if (saveOnShutdown != null) template.settings().saveOnShutdown(saveOnShutdown);
        if (maxPlayerCount != null) template.settings().maxPlayerCount(maxPlayerCount);
        if (minServerCount != null) template.settings().minServerCount(minServerCount);
        if (maxServerCount != null) template.settings().maxServerCount(maxServerCount);
        if (startNewPercentage != null) template.settings().startNewPercentage(startNewPercentage);
        if (autoStart != null) template.settings().autoStart(autoStart);
        if (maxMemory != null) template.settings().maxMemory(maxMemory);
    }

    public static TemplateEditData create() {
        return new TemplateEditData();
    }

    public static TemplateEditData between(TemplateSettings oldSettings, TemplateSettings newSettings) {
        TemplateEditData templateEditData = new TemplateEditData();
        if (oldSettings.lobby() != newSettings.lobby()) templateEditData.lobby = newSettings.lobby();
        if (oldSettings.maintenance() != newSettings.maintenance())
            templateEditData.maintenance = newSettings.maintenance();
        if (oldSettings.staticServers() != newSettings.staticServers())
            templateEditData.staticServers = newSettings.staticServers();
        if (oldSettings.alwaysCopyToStaticServers() != newSettings.alwaysCopyToStaticServers())
            templateEditData.alwaysCopyToStaticServers = newSettings.alwaysCopyToStaticServers();
        if (oldSettings.saveOnShutdown() != newSettings.saveOnShutdown())
            templateEditData.saveOnShutdown = newSettings.saveOnShutdown();
        if (oldSettings.maxPlayerCount() != newSettings.maxPlayerCount())
            templateEditData.maxPlayerCount = newSettings.maxPlayerCount();
        if (oldSettings.minServerCount() != newSettings.minServerCount())
            templateEditData.minServerCount = newSettings.minServerCount();
        if (oldSettings.maxServerCount() != newSettings.maxServerCount())
            templateEditData.maxServerCount = newSettings.maxServerCount();
        if (oldSettings.startNewPercentage() != newSettings.startNewPercentage())
            templateEditData.startNewPercentage = newSettings.startNewPercentage();
        if (oldSettings.autoStart() != newSettings.autoStart()) templateEditData.autoStart = newSettings.autoStart();
        if (oldSettings.maxMemory() != newSettings.maxMemory()) templateEditData.maxMemory = newSettings.maxMemory();
        return templateEditData;
    }
}