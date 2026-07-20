package de.pocketcloud.bridge.component;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.shared.component.BaseCloudPlayer;

import java.util.UUID;

public final class CloudPlayer extends BaseCloudPlayer implements SyncingElement<ICloudPlayer> {

    public CloudPlayer(String name, String address, String xboxUserId, UUID uniqueId, int protocolVersion, String gameVersion, String currentServerName, String currentProxyName) {
        super(name, address, xboxUserId, uniqueId, protocolVersion, gameVersion, currentServerName, currentProxyName);
    }

    public CloudPlayer(String name, String address, String xboxUserId, UUID uniqueId, int protocolVersion, String gameVersion) {
        super(name, address, xboxUserId, uniqueId, protocolVersion, gameVersion);
    }

    @Override
    public void syncIn(ICloudPlayer data) {
        changeCurrentServer(data.currentServerName());
        changeCurrentProxy(data.currentProxyName());
    }

    @Override
    public void syncOut() {

    }
}