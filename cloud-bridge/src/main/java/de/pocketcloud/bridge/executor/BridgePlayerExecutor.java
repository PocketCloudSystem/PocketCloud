package de.pocketcloud.bridge.executor;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.executor.IPlayerExecutor;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.adapter.NativePlayerAdapter;
import de.pocketcloud.network.packet.impl.PlayerKickPacket;
import de.pocketcloud.network.packet.impl.PlayerTextPacket;
import de.pocketcloud.network.packet.impl.PlayerTransferPacket;
import de.pocketcloud.shared.network.packet.type.TextType;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class BridgePlayerExecutor implements IPlayerExecutor {

    @Override
    public void sendMessage(UUID uuid, String message) {
        message(Target.of(uuid), message);
    }

    @Override
    public void sendPopup(UUID uuid, String popup, String subtitle) {
        popup(Target.of(uuid), popup, subtitle);
    }

    @Override
    public void sendTip(UUID uuid, String tip) {
        tip(Target.of(uuid), tip);
    }

    @Override
    public void sendTitle(UUID uuid, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        title(Target.of(uuid), title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendActionbarMessage(UUID uuid, String message, int fadeIn, int stay, int fadeOut) {
        actionbar(Target.of(uuid), message, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendToast(UUID uuid, String title, String body) {
        toast(Target.of(uuid), title, body);
    }

    @Override
    public void kick(UUID uuid, String reason, String disconnectScreenMessage) {
        kick(Target.of(uuid), reason, disconnectScreenMessage);
    }

    @Override
    public boolean transfer(UUID uuid, ICloudServer server, boolean useCustomPlayerCount) {
        return transfer(Target.of(uuid), server, useCustomPlayerCount);
    }

    @Override
    public void sendMessage(String nameOrXuid, String message) {
        message(Target.of(nameOrXuid), message);
    }

    @Override
    public void sendPopup(String nameOrXuid, String popup, String subtitle) {
        popup(Target.of(nameOrXuid), popup, subtitle);
    }

    @Override
    public void sendTip(String nameOrXuid, String tip) {
        tip(Target.of(nameOrXuid), tip);
    }

    @Override
    public void sendTitle(String nameOrXuid, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        title(Target.of(nameOrXuid), title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendActionbarMessage(String nameOrXuid, String message, int fadeIn, int stay, int fadeOut) {
        actionbar(Target.of(nameOrXuid), message, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendToast(String nameOrXuid, String title, String body) {
        toast(Target.of(nameOrXuid), title, body);
    }

    @Override
    public void kick(String nameOrXuid, String reason, String disconnectScreenMessage) {
        kick(Target.of(nameOrXuid), reason, disconnectScreenMessage);
    }

    @Override
    public boolean transfer(String nameOrXuid, ICloudServer server, boolean useCustomPlayerCount) {
        return transfer(Target.of(nameOrXuid), server, useCustomPlayerCount);
    }

    private void message(Target target, String message) {
        handle(
                target,
                (adapter, player) -> adapter.sendMessage(player, message),
                player -> PlayerTextPacket.create(player.name(), message, TextType.MESSAGE).sendPacket()
        );
    }

    private void popup(Target target, String popup, String subtitle) {
        handle(
                target,
                (adapter, player) -> adapter.sendPopup(player, popup, subtitle),
                player -> PlayerTextPacket.create(player.name(), popup, subtitle, TextType.POPUP).sendPacket()
        );
    }

    private void tip(Target target, String tip) {
        handle(
                target,
                (adapter, player) -> adapter.sendTip(player, tip),
                player -> PlayerTextPacket.create(player.name(), tip, TextType.TIP).sendPacket()
        );
    }

    private void title(Target target, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        handle(
                target,
                (adapter, player) -> adapter.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut),
                player -> PlayerTextPacket.create(player.name(), title, subtitle, TextType.TITLE, fadeIn, stay, fadeOut).sendPacket()
        );
    }

    private void actionbar(Target target, String message, int fadeIn, int stay, int fadeOut) {
        handle(
                target,
                (adapter, player) -> adapter.sendActionbarMessage(player, message, fadeIn, stay, fadeOut),
                player -> PlayerTextPacket.create(player.name(), "", message, TextType.ACTION_BAR, fadeIn, stay, fadeOut).sendPacket()
        );
    }

    private void toast(Target target, String title, String body) {
        handle(
                target,
                (adapter, player) -> adapter.sendToast(player, title, body),
                player -> PlayerTextPacket.create(player.name(), title, body, TextType.TOAST).sendPacket()
        );
    }

    private void kick(Target target, String reason, String disconnectScreenMessage) {
        handle(
                target,
                (adapter, player) -> adapter.kick(player, reason, disconnectScreenMessage),
                player -> PlayerKickPacket.create(player.name(), reason, disconnectScreenMessage).sendPacket()
        );
    }

    private boolean transfer(Target target, ICloudServer server, boolean useCustomPlayerCount) {
        if (!canTransfer(server, useCustomPlayerCount)) return false;

        return handle(
                target,
                (adapter, player) -> adapter.transfer(player, server),
                player -> PlayerTransferPacket.create(player.name(), server.name()).sendPacket()
        );
    }

    private boolean canTransfer(ICloudServer server, boolean useCustomPlayerCount) {
        if (!server.status().isOnline() || !server.verificationStatus().isVerified()) return false;

        int maxPlayers = useCustomPlayerCount
                ? server.data().maxPlayers()
                : server.template().settings().maxPlayerCount();

        return server.playerCount() < maxPlayers;
    }

    private <T> boolean handle(Target target, BiConsumer<NativePlayerAdapter<T>, T> localAction, Consumer<ICloudPlayer> remoteAction) {
        NativePlayerAdapter<T> adapter = adapter();

        Optional<T> local = target.findLocal(adapter);
        if (local.isPresent()) {
            localAction.accept(adapter, local.get());
            return true;
        }

        Optional<ICloudPlayer> remote = target.findRemote();
        if (remote.isPresent()) {
            remoteAction.accept(remote.get());
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> NativePlayerAdapter<T> adapter() {
        return (NativePlayerAdapter<T>) CloudBridge.instance().playerAdapter();
    }

    private interface Target {

        <T> Optional<T> findLocal(NativePlayerAdapter<T> adapter);

        Optional<ICloudPlayer> findRemote();

        static Target of(UUID uuid) {
            return new Target() {
                @Override
                public <T> Optional<T> findLocal(NativePlayerAdapter<T> adapter) {
                    return adapter.find(uuid);
                }

                @Override
                public Optional<ICloudPlayer> findRemote() {
                    return CloudBridge.instance().players().get(uuid);
                }
            };
        }

        static Target of(String nameOrXuid) {
            return new Target() {
                @Override
                public <T> Optional<T> findLocal(NativePlayerAdapter<T> adapter) {
                    return adapter.find(nameOrXuid);
                }

                @Override
                public Optional<ICloudPlayer> findRemote() {
                    return CloudBridge.instance().players().get(nameOrXuid);
                }
            };
        }
    }
}