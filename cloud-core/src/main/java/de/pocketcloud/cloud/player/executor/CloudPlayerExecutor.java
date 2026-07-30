package de.pocketcloud.cloud.player.executor;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.executor.IPlayerExecutor;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.event.impl.player.PlayerKickEvent;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.network.packet.impl.PlayerKickPacket;
import de.pocketcloud.network.packet.impl.PlayerTextPacket;
import de.pocketcloud.network.packet.impl.PlayerTransferPacket;
import de.pocketcloud.shared.network.packet.type.TextType;

import java.util.UUID;

public final class CloudPlayerExecutor implements IPlayerExecutor {

    @Override
    public void sendMessage(UUID uuid, String message) {
        PocketCloud.instance().players().get(uuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), message, TextType.MESSAGE));
                    }
                });
    }

    @Override
    public void sendPopup(UUID uuid, String popup, String subtitle) {
        PocketCloud.instance().players().get(uuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), popup, subtitle, TextType.POPUP));
                    }
                });
    }

    @Override
    public void sendTip(UUID uuid, String tip) {
        PocketCloud.instance().players().get(uuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), tip, TextType.TIP));
                    }
                });
    }

    @Override
    public void sendTitle(UUID uuid, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        PocketCloud.instance().players().get(uuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), title, subtitle, TextType.MESSAGE, fadeIn, stay, fadeOut));
                    }
                });
    }

    @Override
    public void sendActionbarMessage(UUID uuid, String message, int fadeIn, int stay, int fadeOut) {
        PocketCloud.instance().players().get(uuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), "", message, TextType.ACTION_BAR, fadeIn, stay, fadeOut));
                    }
                });
    }

    @Override
    public void sendToast(UUID uuid, String title, String body) {
        PocketCloud.instance().players().get(uuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), title, body, TextType.TOAST));
                    }
                });
    }

    @Override
    public void kick(UUID uuid, String reason, String disconnectScreenMessage) {
        PocketCloud.instance().players().get(uuid)
                .ifPresent(p -> {
                    if (new PlayerKickEvent((CloudPlayer) p, reason, disconnectScreenMessage).call().isCancelled()) return;
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerKickPacket.create(p.name(), reason, disconnectScreenMessage));
                    }
                });
    }

    @Override
    public boolean transfer(UUID uuid, ICloudServer server, boolean useCustomPlayerCount) {
        if (!server.status().isOnline() || !server.verificationStatus().isVerified()) return false;
        int maxPlayers = useCustomPlayerCount ? server.data().maxPlayers() : server.template().settings().maxPlayerCount();
        if (server.playerCount() >= maxPlayers) return false;
        ICloudPlayer player = PocketCloud.instance().players().get(uuid).orElse(null);
        if (player == null) return false;
        CloudServer selectedServer = (CloudServer) player.currentProxy().orElse(player.currentServer().orElse(null));
        if (selectedServer != null) {
            selectedServer.sendPacket(PlayerTransferPacket.create(player.name(), server.name()));
        }
        return false;
    }

    @Override
    public void sendMessage(String nameOrXuid, String message) {
        PocketCloud.instance().players().get(nameOrXuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), message, TextType.MESSAGE));
                    }
                });
    }

    @Override
    public void sendPopup(String nameOrXuid, String popup, String subtitle) {
        PocketCloud.instance().players().get(nameOrXuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), popup, subtitle, TextType.POPUP));
                    }
                });
    }

    @Override
    public void sendTip(String nameOrXuid, String tip) {
        PocketCloud.instance().players().get(nameOrXuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), tip, TextType.TIP));
                    }
                });
    }

    @Override
    public void sendTitle(String nameOrXuid, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        PocketCloud.instance().players().get(nameOrXuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), title, subtitle, TextType.MESSAGE, fadeIn, stay, fadeOut));
                    }
                });
    }

    @Override
    public void sendActionbarMessage(String nameOrXuid, String message, int fadeIn, int stay, int fadeOut) {
        PocketCloud.instance().players().get(nameOrXuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), "", message, TextType.ACTION_BAR, fadeIn, stay, fadeOut));
                    }
                });
    }

    @Override
    public void sendToast(String nameOrXuid, String title, String body) {
        PocketCloud.instance().players().get(nameOrXuid)
                .ifPresent(p -> {
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerTextPacket.create(p.name(), title, body, TextType.TOAST));
                    }
                });
    }

    @Override
    public void kick(String nameOrXuid, String reason, String disconnectScreenMessage) {
        PocketCloud.instance().players().get(nameOrXuid)
                .ifPresent(p -> {
                    if (new PlayerKickEvent((CloudPlayer) p, reason, disconnectScreenMessage).call().isCancelled()) return;
                    CloudServer selectedServer = (CloudServer) p.currentProxy().orElse(p.currentServer().orElse(null));
                    if (selectedServer != null) {
                        selectedServer.sendPacket(PlayerKickPacket.create(p.name(), reason, disconnectScreenMessage));
                    }
                });
    }

    @Override
    public boolean transfer(String nameOrXuid, ICloudServer server, boolean useCustomPlayerCount) {
        if (!server.status().isOnline() || !server.verificationStatus().isVerified()) return false;
        int maxPlayers = useCustomPlayerCount ? server.data().maxPlayers() : server.template().settings().maxPlayerCount();
        if (server.playerCount() >= maxPlayers) return false;
        ICloudPlayer player = PocketCloud.instance().players().get(nameOrXuid).orElse(null);
        if (player == null) return false;
        CloudServer selectedServer = (CloudServer) player.currentProxy().orElse(player.currentServer().orElse(null));
        if (selectedServer != null) {
            selectedServer.sendPacket(PlayerTransferPacket.create(player.name(), server.name()));
        }
        return false;
    }
}