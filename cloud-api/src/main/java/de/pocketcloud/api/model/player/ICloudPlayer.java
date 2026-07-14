package de.pocketcloud.api.model.player;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.model.server.ICloudServer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface ICloudPlayer {

    String name();

    String address();

    String xboxUserId();

    UUID uniqueId();

    String currentServerName();

    default Optional<? extends ICloudServer> currentServer() {
        return currentServerName() == null ? Optional.empty() : CloudAPI.instance().servers().get(currentServerName());
    }

    String currentProxyName();

    default Optional<? extends ICloudServer> currentProxy() {
        return currentProxyName() == null ? Optional.empty() : CloudAPI.instance().servers().get(currentProxyName());
    }
}