package de.pocketcloud.cloud.player.executor;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.executor.IPlayerExecutor;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.event.impl.player.PlayerKickEvent;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.network.packet.impl.PlayerKickPacket;
import de.pocketcloud.network.packet.impl.PlayerTextPacket;
import de.pocketcloud.network.packet.impl.PlayerTransferPacket;
import de.pocketcloud.shared.network.packet.type.TextType;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class CloudPlayerExecutor implements IPlayerExecutor {

    @Override
    public void sendMessage(UUID uuid, String message) {
        message(find(uuid), message);
    }

    @Override
    public void sendPopup(UUID uuid, String popup, String subtitle) {
        popup(find(uuid), popup, subtitle);
    }

    @Override
    public void sendTip(UUID uuid, String tip) {
        tip(find(uuid), tip);
    }

    @Override
    public void sendTitle(UUID uuid, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        title(find(uuid), title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendActionbarMessage(UUID uuid, String message, int fadeIn, int stay, int fadeOut) {
        actionbar(find(uuid), message, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendToast(UUID uuid, String title, String body) {
        toast(find(uuid), title, body);
    }

    @Override
    public void kick(UUID uuid, String reason, String disconnectScreenMessage) {
        kick(find(uuid), reason, disconnectScreenMessage);
    }

    @Override
    public boolean transfer(UUID uuid, ICloudServer server, boolean useCustomPlayerCount) {
        return transfer(find(uuid), server, useCustomPlayerCount);
    }

    @Override
    public void sendMessage(String nameOrXuid, String message) {
        message(find(nameOrXuid), message);
    }

    @Override
    public void sendPopup(String nameOrXuid, String popup, String subtitle) {
        popup(find(nameOrXuid), popup, subtitle);
    }

    @Override
    public void sendTip(String nameOrXuid, String tip) {
        tip(find(nameOrXuid), tip);
    }

    @Override
    public void sendTitle(String nameOrXuid, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        title(find(nameOrXuid), title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendActionbarMessage(String nameOrXuid, String message, int fadeIn, int stay, int fadeOut) {
        actionbar(find(nameOrXuid), message, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendToast(String nameOrXuid, String title, String body) {
        toast(find(nameOrXuid), title, body);
    }

    @Override
    public void kick(String nameOrXuid, String reason, String disconnectScreenMessage) {
        kick(find(nameOrXuid), reason, disconnectScreenMessage);
    }

    @Override
    public boolean transfer(String nameOrXuid, ICloudServer server, boolean useCustomPlayerCount) {
        return transfer(find(nameOrXuid), server, useCustomPlayerCount);
    }

    private void message(Optional<? extends ICloudPlayer> player, String message) {
        dispatch(player, (server, p) ->
                server.sendPacket(PlayerTextPacket.create(p.name(), message, TextType.MESSAGE)));
    }

    private void popup(Optional<? extends ICloudPlayer> player, String popup, String subtitle) {
        dispatch(player, (server, p) ->
                server.sendPacket(PlayerTextPacket.create(p.name(), popup, subtitle, TextType.POPUP)));
    }

    private void tip(Optional<? extends ICloudPlayer> player, String tip) {
        dispatch(player, (server, p) ->
                server.sendPacket(PlayerTextPacket.create(p.name(), tip, TextType.TIP)));
    }

    private void title(Optional<? extends ICloudPlayer> player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        dispatch(player, (server, p) ->
                server.sendPacket(PlayerTextPacket.create(p.name(), title, subtitle, TextType.TITLE, fadeIn, stay, fadeOut)));
    }

    private void actionbar(Optional<? extends ICloudPlayer> player, String message, int fadeIn, int stay, int fadeOut) {
        dispatch(player, (server, p) ->
                server.sendPacket(PlayerTextPacket.create(p.name(), "", message, TextType.ACTION_BAR, fadeIn, stay, fadeOut)));
    }

    private void toast(Optional<? extends ICloudPlayer> player, String title, String body) {
        dispatch(player, (server, p) ->
                server.sendPacket(PlayerTextPacket.create(p.name(), title, body, TextType.TOAST)));
    }

    private void kick(Optional<? extends ICloudPlayer> player, String reason, String disconnectScreenMessage) {
        player.ifPresent(p -> {
            if (new PlayerKickEvent((CloudPlayer) p, reason, disconnectScreenMessage).call().isCancelled()) return;

            route(p).ifPresent(server ->
                    server.sendPacket(PlayerKickPacket.create(p.name(), reason, disconnectScreenMessage)));
        });
    }

    private boolean transfer(Optional<? extends ICloudPlayer> player, ICloudServer target, boolean useCustomPlayerCount) {
        if (!canTransfer(target, useCustomPlayerCount)) return false;

        return dispatch(player, (server, p) ->
                server.sendPacket(PlayerTransferPacket.create(p.name(), target.name())));
    }

    private boolean canTransfer(ICloudServer server, boolean useCustomPlayerCount) {
        if (!server.status().isOnline() || !server.verificationStatus().isVerified()) return false;

        int maxPlayers = useCustomPlayerCount
                ? server.data().maxPlayers()
                : server.template().settings().maxPlayerCount();

        return server.playerCount() < maxPlayers;
    }

    private boolean dispatch(Optional<? extends ICloudPlayer> player, BiConsumer<CloudServer, ICloudPlayer> action) {
        if (player.isEmpty()) return false;

        ICloudPlayer p = player.get();
        Optional<CloudServer> server = route(p);
        if (server.isEmpty()) return false;

        action.accept(server.get(), p);
        return true;
    }

    private Optional<CloudServer> route(ICloudPlayer player) {
        return Optional.ofNullable((CloudServer) player.currentProxy().orElse(player.currentServer().orElse(null)));
    }

    private Optional<? extends ICloudPlayer> find(UUID uuid) {
        return PocketCloud.instance().players().get(uuid);
    }

    private Optional<? extends ICloudPlayer> find(String nameOrXuid) {
        return PocketCloud.instance().players().get(nameOrXuid);
    }
}