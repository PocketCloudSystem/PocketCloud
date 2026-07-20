package de.pocketcloud.api.template.util;

import de.pocketcloud.api.component.template.ITemplate;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true)
public class TemplateEditData {

    private Boolean lobby = null;
    private Boolean maintenance = null;
    private Boolean staticServers = null;
    private Boolean alwaysCopyToStaticServers = null;
    private Integer maxPlayerCount = null;
    private Integer minServerCount = null;
    private Integer maxServerCount = null;
    private Double startNewPercentage = null;
    private Boolean autoStart = null;

    /**
     * {@link TemplateEditData#create()}
     */
    private TemplateEditData() {}

    public void applyTo(ITemplate template) {
        if (lobby != null) template.settings().lobby(lobby);
        if (maintenance != null) template.settings().maintenance(maintenance);
        if (staticServers != null) template.settings().staticServers(staticServers);
        if (alwaysCopyToStaticServers != null) template.settings().alwaysCopyToStaticServers(alwaysCopyToStaticServers);
        if (maxPlayerCount != null) template.settings().maxPlayerCount(maxPlayerCount);
        if (minServerCount != null) template.settings().minServerCount(minServerCount);
        if (maxServerCount != null) template.settings().maxServerCount(maxServerCount);
        if (startNewPercentage != null) template.settings().startNewPercentage(startNewPercentage);
        if (autoStart != null) template.settings().autoStart(autoStart);
    }

    public static TemplateEditData create() {
        return new TemplateEditData();
    }
}