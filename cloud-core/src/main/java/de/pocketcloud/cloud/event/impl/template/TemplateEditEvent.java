package de.pocketcloud.cloud.event.impl.template;

import de.pocketcloud.api.template.util.TemplateEditData;
import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.template.Template;
import lombok.Getter;

public class TemplateEditEvent extends TemplateEvent implements Cancelable {

    @Getter
    private final TemplateEditData editData;

    public TemplateEditEvent(Template template, TemplateEditData editData) {
        super(template);
        this.editData = editData;
    }
}