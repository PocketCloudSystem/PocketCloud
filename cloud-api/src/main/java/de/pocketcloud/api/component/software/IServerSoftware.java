package de.pocketcloud.api.component.software;

import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.common.serialization.Writable;

import java.util.Map;

public interface IServerSoftware extends Writable<Map<String, Object>> {

    String name();

    default String normalizedName() {
        return name().toLowerCase().replace(" ", "_");
    }

    String templateType();

    ISoftwareDownload download();

    ISoftwareBinary binary();

    ISoftwareBridge bridge();

    ISoftwareConfig config();

    default TemplateType type() {
        try {
            return TemplateType.valueOf(templateType());
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    default Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }
}