package de.pocketcloud.shared.event.template;

import de.pocketcloud.api.component.template.ITemplate;

public final class TemplateCreatedEvent extends TemplateEvent {

    public TemplateCreatedEvent(ITemplate template) {
        super(template);
    }

    public ITemplate getTemplate() {
        return template;
    }
}