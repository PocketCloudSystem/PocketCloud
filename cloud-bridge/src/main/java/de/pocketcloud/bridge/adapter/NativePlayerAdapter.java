package de.pocketcloud.bridge.adapter;

import de.pocketcloud.api.component.server.ICloudServer;

import java.util.Optional;
import java.util.UUID;

public interface NativePlayerAdapter<T> {

    void sendMessage(T player, String message);

    void sendPopup(T player, String popup, String subtitle);

    void sendTip(T player, String tip);

    void sendTitle(T player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void sendActionbarMessage(T player, String message, int fadeIn, int stay, int fadeOut);

    void sendToast(T player, String title, String body);

    void kick(T player, String reason, String disconnectScreenMessage);

    void transfer(T player, ICloudServer server);

    Optional<T> find(String nameOrXuid);

    Optional<T> find(UUID uuid);
}