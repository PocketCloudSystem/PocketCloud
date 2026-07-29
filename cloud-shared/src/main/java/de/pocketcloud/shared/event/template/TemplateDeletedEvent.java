package de.pocketcloud.shared.event.template;

import de.pocketcloud.api.component.template.ITemplate;

public final class TemplateDeletedEvent extends TemplateEvent {

    public TemplateDeletedEvent(ITemplate template) {
        super(template);
    }

    public ITemplate getTemplate() {
        return template;
    }
}