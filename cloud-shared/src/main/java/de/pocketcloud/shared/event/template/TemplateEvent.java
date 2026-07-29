package de.pocketcloud.shared.event.template;

import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.event.Event;

public abstract class TemplateEvent implements Event {

    protected final ITemplate template;

    public TemplateEvent(ITemplate template) {
        this.template = template;
    }
}