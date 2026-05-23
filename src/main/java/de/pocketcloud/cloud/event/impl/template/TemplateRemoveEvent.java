package de.pocketcloud.cloud.event.impl.template;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.template.Template;

public final class TemplateRemoveEvent extends TemplateEvent implements Cancelable {

    public TemplateRemoveEvent(Template template) {
        super(template);
    }
}