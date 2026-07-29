package de.pocketcloud.cloud.player;

import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.component.BaseCloudPlayer;
import de.pocketcloud.shared.sync.SyncType;

import java.util.Map;
import java.util.UUID;

public final class CloudPlayer extends BaseCloudPlayer implements SyncingElement<CloudPlayer> {

    /**
     * Only meant for the SyncPacket
     */
    private transient boolean markedForRemoval = false;

    public CloudPlayer(String name, String address, String xboxUserId, UUID uniqueId, int protocolVersion, String gameVersion) {
        super(name, address, xboxUserId, uniqueId, protocolVersion, gameVersion);
    }

    public CloudPlayer(String name, String address, String xboxUserId, UUID uniqueId, int protocolVersion, String gameVersion, String currentServerName, String currentProxyName) {
        super(name, address, xboxUserId, uniqueId, protocolVersion, gameVersion);
        this.currentServerName = currentServerName;
        this.currentProxyName = currentProxyName;
    }

    public CloudPlayer markForRemoval() {
        this.markedForRemoval = true;
        return this;
    }

    @Override
    public void syncIn(CloudPlayer data) {}

    @Override
    public void syncOut() {
        SyncPacket.create(SyncType.PLAYER, data -> data.writeAll(this, markedForRemoval)).broadcast();
    }

    @Override
    public void changeCurrentServer(String serverName) {
        CloudLogger.get().debug("Changing current server of " + name + " to " + serverName);
        this.currentServerName = serverName;
        syncOut();
    }

    @Override
    public void resetCurrentServer() {
        CloudLogger.get().debug("Changing current server of " + name + " to NULL");
        super.resetCurrentServer();
        syncOut();
    }

    @Override
    public void changeCurrentProxy(String serverName) {
        CloudLogger.get().debug("Changing current proxy of " + name + " to " + serverName);
        this.currentProxyName = serverName;
        syncOut();
    }

    @Override
    public void resetCurrentProxy() {
        CloudLogger.get().debug("Changing current proxy of " + name + " to NULL");
        super.resetCurrentProxy();
        syncOut();
    }

    public static CloudPlayer read(Map<String, Object> data) {
        return MapperUtils.fromMap(data, CloudPlayer.class);
    }
}