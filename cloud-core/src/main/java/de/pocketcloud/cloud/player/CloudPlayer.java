package de.pocketcloud.cloud.player;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.bridge.BridgeClusterModule;
import de.pocketcloud.cloud.cluster.state.ClusterStateStore;
import de.pocketcloud.cloud.cluster.state.StateKeys;
import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
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

    public CloudPlayer markForRemoval() {
        this.markedForRemoval = true;
        return this;
    }

    @Override
    public void syncIn(CloudPlayer data) {}

    @Override
    public void syncOut() {
        syncLocal();
        ClusterStateStore state = PocketCloud.instance().cluster().state();
        if (markedForRemoval) state.remove(StateKeys.PLAYERS, name());
        else state.put(StateKeys.PLAYERS, name(), write());
        BridgeClusterModule.get().ifPresent(module -> module.publishPlayer(this, markedForRemoval));
    }

    /**
     * Only sends the player to the bridges of this node (used for changes that came from another node).
     */
    public void syncLocal() {
        SyncPacket.create(SyncType.PLAYER, data -> data.writeAll(this, markedForRemoval)).broadcast();
    }

    /**
     * Takes over the server and proxy of the same player reported by another node.
     */
    public void applyRemote(CloudPlayer other) {
        this.currentServerName = other.currentServerName;
        this.currentProxyName = other.currentProxyName;
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