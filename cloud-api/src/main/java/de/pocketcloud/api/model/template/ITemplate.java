package de.pocketcloud.api.model.template;

import de.pocketcloud.api.model.software.IServerSoftware;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.settings.TemplateSettings;

public interface ITemplate {

    String name();

    TemplateSettings settings();

    TemplateType templateType();

    IServerSoftware serverSoftware();
}