package de.pocketcloud.shared.component;

import de.pocketcloud.api.component.player.ICloudPlayer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.UUID;

@Getter
@Setter
@Accessors(fluent = true)
public class BaseCloudPlayer implements ICloudPlayer {

    protected final String name;
    protected final String address;
    protected final String xboxUserId;
    protected final UUID uniqueId;
    protected final int protocolVersion;
    protected final String gameVersion;
    @Setter(AccessLevel.NONE)
    protected String currentServerName = null;
    @Setter(AccessLevel.NONE)
    protected String currentProxyName = null;

    public BaseCloudPlayer(String name, String address, String xboxUserId, UUID uniqueId, int protocolVersion, String gameVersion) {
        this.name = name;
        this.address = address;
        this.xboxUserId = xboxUserId;
        this.uniqueId = uniqueId;
        this.protocolVersion = protocolVersion;
        this.gameVersion = gameVersion;
    }

    @Override
    public void changeCurrentServer(String serverName) {
        this.currentServerName = serverName;
    }

    @Override
    public void resetCurrentServer() {
        this.currentServerName = null;
    }

    @Override
    public void changeCurrentProxy(String serverName) {
        this.currentProxyName = serverName;
    }

    @Override
    public void resetCurrentProxy() {
        this.currentProxyName = null;
    }
}