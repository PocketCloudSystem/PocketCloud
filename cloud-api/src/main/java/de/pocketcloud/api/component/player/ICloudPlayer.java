package de.pocketcloud.api.component.player;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.serialization.Writable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ICloudPlayer extends Writable<Map<String, Object>> {

    default void sendMessage(String message) {
        CloudAPI.instance().playerExecutor().sendMessage(uniqueId(), message);
    }

    default void sendPopup(String popup) {
        CloudAPI.instance().playerExecutor().sendPopup(uniqueId(), popup);
    }

    default void sendPopup(String popup, String subtitle) {
        CloudAPI.instance().playerExecutor().sendPopup(uniqueId(), popup, subtitle);
    }

    default void sendTip(String tip) {
        CloudAPI.instance().playerExecutor().sendTip(uniqueId(), tip);
    }

    default void sendTitle(String title) {
        CloudAPI.instance().playerExecutor().sendTitle(uniqueId(), title);
    }

    default void sendTitle(String title, String subtitle) {
        CloudAPI.instance().playerExecutor().sendTitle(uniqueId(), title, subtitle);
    }

    default void sendTitle(String title, String subtitle, int fadeIn) {
        CloudAPI.instance().playerExecutor().sendTitle(uniqueId(), title, subtitle, fadeIn);
    }

    default void sendTitle(String title, String subtitle, int fadeIn, int stay) {
        CloudAPI.instance().playerExecutor().sendTitle(uniqueId(), title, subtitle, fadeIn, stay);
    }

    default void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        CloudAPI.instance().playerExecutor().sendTitle(uniqueId(), title, subtitle, fadeIn, stay, fadeOut);
    }

    default void sendActionbarMessage(String message) {
        CloudAPI.instance().playerExecutor().sendActionbarMessage(uniqueId(), message);
    }

    default void sendActionbarMessage(String message, int fadeIn) {
        CloudAPI.instance().playerExecutor().sendActionbarMessage(uniqueId(), message, fadeIn);
    }

    default void sendActionbarMessage(String message, int fadeIn, int stay) {
        CloudAPI.instance().playerExecutor().sendActionbarMessage(uniqueId(), message, fadeIn, stay);
    }

    default void sendActionbarMessage(String message, int fadeIn, int stay, int fadeOut) {
        CloudAPI.instance().playerExecutor().sendActionbarMessage(uniqueId(), message, fadeIn, stay, fadeOut);
    }

    default void sendToast(String title, String body) {
        CloudAPI.instance().playerExecutor().sendToast(uniqueId(), title, body);
    }

    default void kick() {
        CloudAPI.instance().playerExecutor().kick(uniqueId());
    }

    default void kick(String reason) {
        CloudAPI.instance().playerExecutor().kick(uniqueId(), reason);
    }

    default void kick(String reason, String disconnectScreenMessage) {
        CloudAPI.instance().playerExecutor().kick(uniqueId(), reason, disconnectScreenMessage);
    }

    default void transfer(ICloudServer server) {
        CloudAPI.instance().playerExecutor().transfer(uniqueId(), server);
    }
    
    String name();

    String address();

    String xboxUserId();

    UUID uniqueId();

    int protocolVersion();

    String gameVersion();

    String currentServerName();

    default void changeCurrentServer(ICloudServer server) {
        if (server != null) changeCurrentServer(server.name());
        else resetCurrentServer();
    }

    void changeCurrentServer(String serverName);

    void resetCurrentServer();

    default Optional<ICloudServer> currentServer() {
        return currentServerName() == null ? Optional.empty() : CloudAPI.instance().servers().get(currentServerName());
    }

    String currentProxyName();

    default void changeCurrentProxy(ICloudServer server) {
        if (server != null) changeCurrentProxy(server.name());
        else resetCurrentProxy();
    }

    void changeCurrentProxy(String serverName);

    void resetCurrentProxy();

    default Optional<ICloudServer> currentProxy() {
        return currentProxyName() == null ? Optional.empty() : CloudAPI.instance().servers().get(currentProxyName());
    }

    default Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }
}