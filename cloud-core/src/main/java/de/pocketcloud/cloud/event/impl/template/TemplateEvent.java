package de.pocketcloud.cloud.event.impl.template;

import de.pocketcloud.cloud.event.Event;
import de.pocketcloud.cloud.template.Template;
import lombok.Getter;

public abstract class TemplateEvent extends Event {

    @Getter
    private final Template template;

    public TemplateEvent(Template template) {
        this.template = template;
    }
}