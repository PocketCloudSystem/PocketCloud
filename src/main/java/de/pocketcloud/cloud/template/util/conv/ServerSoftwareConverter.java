package de.pocketcloud.cloud.template.util.conv;

import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.server.software.ServerSoftwareManager;
import de.pocketcloud.cloud.util.mapper.MapKeyConverter;

public class ServerSoftwareConverter implements MapKeyConverter<ServerSoftware, String> {

    @Override
    public String toValue(ServerSoftware software) {
        return software.name();
    }

    @Override
    public ServerSoftware fromValue(String value) {
        return ServerSoftwareManager.getInstance().get(value);
    }
}