package de.pocketcloud.cloud.player;

import de.pocketcloud.api.model.player.ICloudPlayer;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.player.PlayerKickEvent;
import de.pocketcloud.network.packet.type.TextType;
import de.pocketcloud.cloud.network.packet.impl.PlayerKickPacket;
import de.pocketcloud.cloud.network.packet.impl.PlayerSyncPacket;
import de.pocketcloud.cloud.network.packet.impl.PlayerTextPacket;
import de.pocketcloud.cloud.network.packet.impl.PlayerTransferPacket;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.common.serialization.Writable;
import de.pocketcloud.common.mapper.MapperUtils;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
public final class CloudPlayer implements Writable<Map<String, Object>>, ICloudPlayer {

    private final String name;
    private final String address;
    private final String xboxUserId;
    private final UUID uniqueId;
    private String currentServerName;
    private String currentProxyName;

    public CloudPlayer(String name, String address, String xboxUserId, UUID uniqueId, String currentServerName, String currentProxyName) {
        this.name = name;
        this.address = address;
        this.xboxUserId = xboxUserId;
        this.uniqueId = uniqueId;
        this.currentServerName = currentServerName;
        this.currentProxyName = currentProxyName;
    }

    public CloudPlayer(String name, String address, String xboxUserId, UUID uniqueId) {
        this(name, address, xboxUserId, uniqueId, null, null);
    }

    public void setCurrentServer(CloudServer currentServer) {
        CloudLogger.get().debug("Changing current server of " + name + " to " + (currentServer != null ? currentServer.name() : "NULL"));
        this.currentServerName = currentServer != null ? currentServer.name() : null;
        PlayerSyncPacket.create(this, false).broadcastPacket();
    }

    public void setCurrentProxy(CloudServer currentProxy) {
        CloudLogger.get().debug("Changing current proxy of " + name + " to " + (currentProxy != null ? currentProxy.name() : "NULL"));
        this.currentProxyName = currentProxy != null ? currentProxy.name() : null;
    }

    public void kick(String reason, String disconnectScreenMessage) {
        if (currentServer().isEmpty()) return;
        CloudLogger.get().debug("Kicking {}, reason: {}", name, reason.isEmpty() ? "NULL" : reason);
        var ev = new PlayerKickEvent(this, reason, disconnectScreenMessage);
        ev.call();
        if (ev.isCancelled()) return;
        currentServer().flatMap(CloudServer::client).ifPresent(c -> PlayerKickPacket.create(name, reason, disconnectScreenMessage).sendPacket(c));
    }

    public void kick(String reason) {
        kick(reason, "");
    }

    public void transfer(CloudServer server) {
        var proxy = currentProxy();
        var current = currentServer();

        proxy.ifPresentOrElse(
                p -> p.sendPacket(PlayerTransferPacket.create(name, server.name())),
                () -> current.ifPresent(c -> c.sendPacket(PlayerTransferPacket.create(name, server.name())))
        );
    }

    public void send(String message, TextType textType) {
        if (currentServer().isEmpty() && currentProxy().isEmpty()) return;
        CloudLogger.get().debug("Sending text ({}) to {}", textType.name(), name);
        var target = currentProxy().orElse(currentServer().orElse(null));
        if (target == null) return;
        target.client().ifPresent(c -> PlayerTextPacket.create(name, message, textType).sendPacket(c));
    }

    public void sendMessage(String message) {
        send(message, TextType.MESSAGE);
    }

    public void sendPopup(String message) {
        send(message, TextType.POPUP);
    }

    public void sendTip(String message) {
        send(message, TextType.TIP);
    }

    public void sendTitle(String message) {
        send(message, TextType.TITLE);
    }

    public void sendActionBarMessage(String message) {
        send(message, TextType.ACTION_BAR);
    }

    public void sendToastNotification(String title, String body) {
        send(title + "\n" + body, TextType.TOAST);
    }

    @Override
    public Optional<CloudServer> currentServer() {
        return currentServerName != null ? PocketCloud.instance().servers().get(currentServerName) : Optional.empty();
    }

    @Override
    public Optional<CloudServer> currentProxy() {
        return currentProxyName != null ? PocketCloud.instance().servers().get(currentProxyName) : Optional.empty();
    }

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static CloudPlayer read(Map<String, Object> data) {
        return MapperUtils.fromMap(data, CloudPlayer.class);
    }
}