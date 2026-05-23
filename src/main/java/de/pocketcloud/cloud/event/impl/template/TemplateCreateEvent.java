package de.pocketcloud.cloud.event.impl.template;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.template.Template;

public final class TemplateCreateEvent extends TemplateEvent implements Cancelable {

    public TemplateCreateEvent(Template template) {
        super(template);
    }
}