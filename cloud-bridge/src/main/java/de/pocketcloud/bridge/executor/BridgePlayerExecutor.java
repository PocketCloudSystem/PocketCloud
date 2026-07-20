package de.pocketcloud.bridge.executor;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.executor.IPlayerExecutor;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.adapter.NativePlayerAdapter;

import java.util.UUID;
import java.util.function.BiConsumer;

public final class BridgePlayerExecutor implements IPlayerExecutor {

    @Override
    public void sendMessage(UUID uuid, String message) {
        handle(uuid, (adapter, player) -> adapter.sendMessage(player, message));
    }

    @Override
    public void sendPopup(UUID uuid, String popup, String subtitle) {
        handle(uuid, (adapter, player) -> adapter.sendPopup(player, popup, subtitle));
    }

    @Override
    public void sendTip(UUID uuid, String tip) {
        handle(uuid, (adapter, player) -> adapter.sendTip(player, tip));
    }

    @Override
    public void sendTitle(UUID uuid, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        handle(uuid, (adapter, player) -> adapter.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut));
    }

    @Override
    public void sendActionbarMessage(UUID uuid, String message, int fadeIn, int stay, int fadeOut) {
        handle(uuid, (adapter, player) -> adapter.sendActionbarMessage(player, message, fadeIn, stay, fadeOut));
    }

    @Override
    public void sendToast(UUID uuid, String title, String body) {
        handle(uuid, (adapter, player) -> adapter.sendToast(player, title, body));
    }

    @Override
    public void kick(UUID uuid, String reason, String disconnectScreenMessage) {
        handle(uuid, (adapter, player) -> adapter.kick(player, reason, disconnectScreenMessage));
    }

    @Override
    public void transfer(UUID uuid, ICloudServer server) {
        handle(uuid, (adapter, player) -> adapter.transfer(player, server));
    }

    @Override
    public void sendMessage(String nameOrXuid, String message) {
        handle(nameOrXuid, (adapter, player) -> adapter.sendMessage(player, message));
    }

    @Override
    public void sendPopup(String nameOrXuid, String popup, String subtitle) {
        handle(nameOrXuid, (adapter, player) -> adapter.sendPopup(player, popup, subtitle));
    }

    @Override
    public void sendTip(String nameOrXuid, String tip) {
        handle(nameOrXuid, (adapter, player) -> adapter.sendTip(player, tip));
    }

    @Override
    public void sendTitle(String nameOrXuid, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        handle(nameOrXuid, (adapter, player) -> adapter.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut));
    }

    @Override
    public void sendActionbarMessage(String nameOrXuid, String message, int fadeIn, int stay, int fadeOut) {
        handle(nameOrXuid, (adapter, player) -> adapter.sendActionbarMessage(player, message, fadeIn, stay, fadeOut));
    }

    @Override
    public void sendToast(String nameOrXuid, String title, String body) {
        handle(nameOrXuid, (adapter, player) -> adapter.sendToast(player, title, body));
    }

    @Override
    public void kick(String nameOrXuid, String reason, String disconnectScreenMessage) {
        handle(nameOrXuid, (adapter, player) -> adapter.kick(player, reason, disconnectScreenMessage));
    }

    @Override
    public void transfer(String nameOrXuid, ICloudServer server) {
        handle(nameOrXuid, (adapter, player) -> adapter.transfer(player, server));
    }

    private <T> void handle(UUID uuid, BiConsumer<NativePlayerAdapter<T>, T> action) {
        NativePlayerAdapter<T> adapter = adapter();
        adapter.find(uuid).ifPresent(player -> action.accept(adapter, player));
    }

    private <T> void handle(String nameOrXuid, BiConsumer<NativePlayerAdapter<T>, T> action) {
        NativePlayerAdapter<T> adapter = adapter();
        adapter.find(nameOrXuid).ifPresent(player -> action.accept(adapter, player));
    }

    @SuppressWarnings("unchecked")
    private <T> NativePlayerAdapter<T> adapter() {
        return (NativePlayerAdapter<T>) CloudBridge.instance().playerAdapter();
    }
}