package de.pocketcloud.shared.converter;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.common.serialization.annotation.MapKeyConverter;

public final class SoftwareConverter implements MapKeyConverter<IServerSoftware, String> {

    @Override
    public String toValue(IServerSoftware obj) {
        return obj.name();
    }

    @Override
    public IServerSoftware fromValue(String value) {
        return CloudAPI.instance().softwares().get(value).orElse(null);
    }
}