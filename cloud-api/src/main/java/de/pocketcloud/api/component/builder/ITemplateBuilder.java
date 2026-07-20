package de.pocketcloud.api.component.builder;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.template.TemplateType;

public interface ITemplateBuilder extends IComponentBuilder<ITemplate> {

    ITemplateBuilder name(String name);

    ITemplateBuilder lobby(boolean lobby);

    ITemplateBuilder maintenance(boolean maintenance);

    ITemplateBuilder staticServers(boolean staticServers);

    ITemplateBuilder alwaysCopyToStaticServers(boolean alwaysCopyToStaticServers);

    ITemplateBuilder saveOnShutdown(boolean saveOnShutdown);

    ITemplateBuilder maxPlayerCount(int maxPlayerCount);

    ITemplateBuilder minServerCount(int minServerCount);

    ITemplateBuilder maxServerCount(int maxServerCount);

    ITemplateBuilder startNewPercentage(double startPercentage);

    ITemplateBuilder autoStart(boolean autoStart);

    ITemplateBuilder type(TemplateType type);

    ITemplateBuilder software(IServerSoftware software);
}