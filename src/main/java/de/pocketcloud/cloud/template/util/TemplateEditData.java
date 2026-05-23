package de.pocketcloud.cloud.template.util;

import de.pocketcloud.cloud.template.Template;

public class TemplateEditData {

    private Boolean lobby = null;
    private Boolean maintenance = null;
    private Boolean staticServers = null;
    private Boolean alwaysCopyToStaticServers = null;
    private Integer maxPlayerCount = null;
    private Integer minServerCount = null;
    private Integer maxServerCount = null;
    private Float startNewPercentage = null;
    private Boolean autoStart = null;

    /**
     * {@link TemplateEditData#create()}
     */
    private TemplateEditData() {}

    public void applyTo(Template template) {
        if (lobby != null) template.setLobby(lobby);
        if (maintenance != null) template.setMaintenance(maintenance);
        if (staticServers != null) template.setStaticServers(staticServers);
        if (alwaysCopyToStaticServers != null) template.setAlwaysCopyToStaticServers(alwaysCopyToStaticServers);
        if (maxPlayerCount != null) template.setMaxPlayerCount(maxPlayerCount);
        if (minServerCount != null) template.setMinServerCount(minServerCount);
        if (maxServerCount != null) template.setMaxServerCount(maxServerCount);
        if (startNewPercentage != null) template.setStartNewPercentage(startNewPercentage);
        if (autoStart != null) template.setAutoStart(autoStart);
    }

    public TemplateEditData lobby(Boolean lobby) {
        this.lobby = lobby;
        return this;
    }

    public TemplateEditData maintenance(Boolean maintenance) {
        this.maintenance = maintenance;
        return this;
    }

    public TemplateEditData staticServers(Boolean staticServers) {
        this.staticServers = staticServers;
        return this;
    }

    public TemplateEditData alwaysCopyToStaticServers(Boolean alwaysCopyToStaticServers) {
        this.alwaysCopyToStaticServers = alwaysCopyToStaticServers;
        return this;
    }

    public TemplateEditData maxPlayerCount(Integer maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
        return this;
    }

    public TemplateEditData minServerCount(Integer minServerCount) {
        this.minServerCount = minServerCount;
        return this;
    }

    public TemplateEditData maxServerCount(Integer maxServerCount) {
        this.maxServerCount = maxServerCount;
        return this;
    }

    public TemplateEditData startNewPercentage(Float startNewPercentage) {
        this.startNewPercentage = startNewPercentage;
        return this;
    }

    public TemplateEditData autoStart(Boolean autoStart) {
        this.autoStart = autoStart;
        return this;
    }

    public static TemplateEditData create() {
        return new TemplateEditData();
    }
}