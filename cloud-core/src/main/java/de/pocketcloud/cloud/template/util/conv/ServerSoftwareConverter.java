package de.pocketcloud.cloud.template.util.conv;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.common.mapper.MapKeyConverter;
import de.pocketcloud.shared.component.software.ServerSoftware;

public class ServerSoftwareConverter implements MapKeyConverter<ServerSoftware, String> {

    @Override
    public String toValue(ServerSoftware software) {
        return software.name();
    }

    @Override
    public ServerSoftware fromValue(String value) {
        return PocketCloud.instance().softwareList().get(value);
    }
}