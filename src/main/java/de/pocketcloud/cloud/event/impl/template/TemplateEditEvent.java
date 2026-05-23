package de.pocketcloud.cloud.event.impl.template;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.util.TemplateEditData;
import lombok.Getter;

public final class TemplateEditEvent extends TemplateEvent implements Cancelable {

    @Getter
    private final TemplateEditData editData;

    public TemplateEditEvent(Template template, TemplateEditData editData) {
        super(template);
        this.editData = editData;
    }
}