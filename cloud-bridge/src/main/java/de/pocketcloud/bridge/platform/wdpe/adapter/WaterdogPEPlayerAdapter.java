package de.pocketcloud.bridge.platform.wdpe.adapter;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.bridge.adapter.NativePlayerAdapter;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket;

import java.util.Optional;
import java.util.UUID;

public final class WaterdogPEPlayerAdapter implements NativePlayerAdapter<ProxiedPlayer> {

    @Override
    public void sendMessage(ProxiedPlayer player, String message) {
        player.sendMessage(message);
    }

    @Override
    public void sendPopup(ProxiedPlayer player, String popup, String subtitle) {
        player.sendPopup(popup, subtitle);
    }

    @Override
    public void sendTip(ProxiedPlayer player, String tip) {
        player.sendTip(tip);
    }

    @Override
    public void sendTitle(ProxiedPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendActionbarMessage(ProxiedPlayer player, String message, int fadeIn, int stay, int fadeOut) {
        player.sendPacket(null);
        SetTitlePacket packet = new SetTitlePacket();
        packet.setTitleType(SetTitlePacket.TitleType.ACTIONBAR);
        packet.setXuid(player.getXuid());
        packet.setPlatformOnlineId("");
        packet.setFadeInTime(fadeIn);
        packet.setStayTime(fadeIn);
        packet.setFadeOutTime(fadeOut);
        player.sendPacket(packet);
    }

    @Override
    public void sendToast(ProxiedPlayer player, String title, String body) {
        player.sendToastMessage(title, body);
    }

    @Override
    public void kick(ProxiedPlayer player, String reason, String disconnectScreenMessage) {
        player.disconnect(disconnectScreenMessage);
    }

    @Override
    public void transfer(ProxiedPlayer player, ICloudServer server) {
        ServerInfo info = player.getProxy().getServerInfo(server.name());
        if (info != null) player.redirectServer(info);
    }

    @Override
    public Optional<ProxiedPlayer> find(String nameOrXuid) {
        ProxiedPlayer byName = ProxyServer.getInstance().getPlayer(nameOrXuid);
        if (byName != null) return Optional.of(byName);
        return ProxyServer.getInstance().getPlayers().values().stream().filter(p -> p.getXuid().equals(nameOrXuid)).findFirst();
    }

    @Override
    public Optional<ProxiedPlayer> find(UUID uuid) {
        return Optional.ofNullable(ProxyServer.getInstance().getPlayer(uuid));
    }
}