package de.pocketcloud.bridge.component;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import de.pocketcloud.shared.component.BaseCloudPlayer;

import java.util.UUID;

public final class CloudPlayer extends BaseCloudPlayer implements SyncingElement<ICloudPlayer> {

    public CloudPlayer(String name, String address, String xboxUserId, UUID uniqueId, int protocolVersion, String gameVersion) {
        this(name, address, xboxUserId, uniqueId, protocolVersion, gameVersion, null, null);
    }

    @MapCreator
    public CloudPlayer(
            @MapKey(name = "name") String name,
            @MapKey(name = "address") String address,
            @MapKey(name = "xboxUserId") String xboxUserId,
            @MapKey(name = "uniqueId") UUID uniqueId,
            @MapKey(name = "protocolVersion") int protocolVersion,
            @MapKey(name = "gameVersion") String gameVersion,
            @MapKey(name = "currentServerName") String currentServerName,
            @MapKey(name = "currentProxyName") String currentProxyName
    ) {
        super(name, address, xboxUserId, uniqueId, protocolVersion, gameVersion);
        this.currentServerName = currentServerName;
        this.currentProxyName = currentProxyName;
    }

    @Override
    public void syncIn(ICloudPlayer data) {
        changeCurrentServer(data.currentServerName());
        changeCurrentProxy(data.currentProxyName());
    }

    @Override
    public void syncOut() {}
}