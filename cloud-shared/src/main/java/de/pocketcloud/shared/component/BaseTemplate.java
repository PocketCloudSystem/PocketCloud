package de.pocketcloud.shared.component;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.settings.TemplateSettings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class BaseTemplate implements ITemplate {

    protected final String name;
    protected final TemplateSettings settings;
    protected final TemplateType templateType;
    protected final IServerSoftware serverSoftware;
}