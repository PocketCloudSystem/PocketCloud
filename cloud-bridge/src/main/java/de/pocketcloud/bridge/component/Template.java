package de.pocketcloud.bridge.component;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.settings.TemplateSettings;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import de.pocketcloud.shared.component.BaseTemplate;
import de.pocketcloud.shared.component.software.ServerSoftware;
import de.pocketcloud.shared.converter.SoftwareConverter;

public final class Template extends BaseTemplate implements SyncingElement<ITemplate> {

    @MapCreator
    public Template(
            @MapKey(name = "name") String name,
            @MapKey(name = "settings") TemplateSettings settings,
            @MapKey(name = "templateType") TemplateType templateType,
            @MapKey(name = "serverSoftware", converter = SoftwareConverter.class) IServerSoftware serverSoftware
    ) {
        super(name, settings, templateType, serverSoftware);
    }

    @Override
    public void syncIn(ITemplate data) {
        settings.applyFrom(data.settings());
    }

    @Override
    public void syncOut() {}

    @Override
    public ServerSoftware serverSoftware() {
        return (ServerSoftware) serverSoftware;
    }
}