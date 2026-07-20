package de.pocketcloud.bridge.component;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.settings.TemplateSettings;
import de.pocketcloud.shared.component.BaseTemplate;

public final class Template extends BaseTemplate implements SyncingElement<ITemplate> {

    public Template(String name, TemplateSettings settings, TemplateType templateType, IServerSoftware serverSoftware) {
        super(name, settings, templateType, serverSoftware);
    }

    @Override
    public void syncIn(ITemplate data) {
        settings.applyFrom(data.settings());
    }

    @Override
    public void syncOut() {

    }
}