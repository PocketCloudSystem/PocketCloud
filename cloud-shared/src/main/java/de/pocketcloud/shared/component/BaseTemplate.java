package de.pocketcloud.shared.component;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.settings.TemplateSettings;
import de.pocketcloud.common.serialization.annotation.MapKey;
import de.pocketcloud.shared.converter.SoftwareConverter;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class BaseTemplate implements ITemplate {

    protected final String name;
    protected final TemplateSettings settings;
    protected final TemplateType templateType;
    @MapKey(converter = SoftwareConverter.class)
    protected final IServerSoftware serverSoftware;

    public BaseTemplate(String name, TemplateSettings settings, TemplateType templateType, IServerSoftware serverSoftware) {
        this.name = name;
        this.settings = settings;
        this.templateType = templateType;
        this.serverSoftware = serverSoftware;
    }
}