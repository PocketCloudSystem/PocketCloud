package de.pocketcloud.shared.event.template;

import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.template.util.TemplateEditData;
import lombok.Getter;

@Getter
public final class TemplateEditedEvent extends TemplateEvent {

    private final TemplateEditData templateEditData;

    public TemplateEditedEvent(ITemplate template, TemplateEditData templateEditData) {
        super(template);
        this.templateEditData = templateEditData;
    }

    public ITemplate getTemplate() {
        return template;
    }
}