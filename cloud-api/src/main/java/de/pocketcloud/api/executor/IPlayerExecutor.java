package de.pocketcloud.api.executor;

import de.pocketcloud.api.component.server.ICloudServer;

import java.util.UUID;

public interface IPlayerExecutor {

    void sendMessage(UUID uuid, String message);

    default void sendPopup(UUID uuid, String popup) {
        sendPopup(uuid, popup, null);
    }

    void sendPopup(UUID uuid, String popup, String subtitle);

    void sendTip(UUID uuid, String tip);

    default void sendTitle(UUID uuid, String title) {
        sendTitle(uuid, title, null, 20, 20, 5);
    }

    default void sendTitle(UUID uuid, String title, String subtitle) {
        sendTitle(uuid, title, subtitle, 20, 20, 5);
    }

    default void sendTitle(UUID uuid, String title, String subtitle, int fadeIn) {
        sendTitle(uuid, title, subtitle, fadeIn, 20, 5);
    }

    default void sendTitle(UUID uuid, String title, String subtitle, int fadeIn, int stay) {
        sendTitle(uuid, title, subtitle, fadeIn, stay, 5);
    }

    void sendTitle(UUID uuid, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    default void sendActionbarMessage(UUID uuid, String message) {
        sendActionbarMessage(uuid, message, 1, 0, 1);
    }

    default void sendActionbarMessage(UUID uuid, String message, int fadeIn) {
        sendActionbarMessage(uuid, message, fadeIn, 0, 1);
    }

    default void sendActionbarMessage(UUID uuid, String message, int fadeIn, int stay) {
        sendActionbarMessage(uuid, message, fadeIn, stay, 1);
    }

    void sendActionbarMessage(UUID uuid, String message, int fadeIn, int stay, int fadeOut);

    void sendToast(UUID uuid, String title, String body);

    default void kick(UUID uuid) {
        kick(uuid, null, null);
    }

    default void kick(UUID uuid, String reason) {
        kick(uuid, reason, reason);
    }

    void kick(UUID uuid, String reason, String disconnectScreenMessage);

    void transfer(UUID uuid, ICloudServer server);

    void sendMessage(String nameOrXuid, String message);

    default void sendPopup(String nameOrXuid, String popup) {
        sendPopup(nameOrXuid, popup, null);
    }

    void sendPopup(String nameOrXuid, String popup, String subtitle);

    void sendTip(String nameOrXuid, String tip);

    default void sendTitle(String nameOrXuid, String title) {
        sendTitle(nameOrXuid, title, null, 20, 20, 5);
    }

    default void sendTitle(String nameOrXuid, String title, String subtitle) {
        sendTitle(nameOrXuid, title, subtitle, 20, 20, 5);
    }

    default void sendTitle(String nameOrXuid, String title, String subtitle, int fadeIn) {
        sendTitle(nameOrXuid, title, subtitle, fadeIn, 20, 5);
    }

    default void sendTitle(String nameOrXuid, String title, String subtitle, int fadeIn, int stay) {
        sendTitle(nameOrXuid, title, subtitle, fadeIn, stay, 5);
    }

    void sendTitle(String nameOrXuid, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    default void sendActionbarMessage(String nameOrXuid, String message) {
        sendActionbarMessage(nameOrXuid, message, 1, 0, 1);
    }

    default void sendActionbarMessage(String nameOrXuid, String message, int fadeIn) {
        sendActionbarMessage(nameOrXuid, message, fadeIn, 0, 1);
    }

    default void sendActionbarMessage(String nameOrXuid, String message, int fadeIn, int stay) {
        sendActionbarMessage(nameOrXuid, message, fadeIn, stay, 1);
    }

    void sendActionbarMessage(String nameOrXuid, String message, int fadeIn, int stay, int fadeOut);

    void sendToast(String nameOrXuid, String title, String body);

    default void kick(String nameOrXuid) {
        kick(nameOrXuid, null, null);
    }

    default void kick(String nameOrXuid, String reason) {
        kick(nameOrXuid, reason, reason);
    }

    void kick(String nameOrXuid, String reason, String disconnectScreenMessage);

    void transfer(String nameOrXuid, ICloudServer server);
}