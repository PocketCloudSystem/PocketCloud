package de.pocketcloud.cloud.template.util.conv;

import de.pocketcloud.cloud.template.TemplateType;
import de.pocketcloud.cloud.util.mapper.MapKeyConverter;

public class TemplateTypeConverter implements MapKeyConverter<TemplateType, String> {

    @Override
    public String toValue(TemplateType type) {
        return type.name();
    }

    @Override
    public TemplateType fromValue(String value) {
        return TemplateType.valueOf(value);
    }
}