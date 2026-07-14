package de.pocketcloud.cloud.template.util.conv;

import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.common.mapper.MapKeyConverter;

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