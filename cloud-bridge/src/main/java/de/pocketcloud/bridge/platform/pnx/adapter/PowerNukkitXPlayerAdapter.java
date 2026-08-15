package de.pocketcloud.bridge.platform.pnx.adapter;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.bridge.adapter.NativePlayerAdapter;
import org.cloudburstmc.protocol.bedrock.packet.TransferPacket;
import org.powernukkitx.Player;
import org.powernukkitx.Server;

import java.util.Optional;
import java.util.UUID;

public final class PowerNukkitXPlayerAdapter implements NativePlayerAdapter<Player> {

    @Override
    public void sendMessage(Player player, String message) {
        player.sendMessage(message);
    }

    @Override
    public void sendPopup(Player player, String popup, String subtitle) {
        player.sendPopup(popup, subtitle);
    }

    @Override
    public void sendTip(Player player, String tip) {
        player.sendTip(tip);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendActionbarMessage(Player player, String message, int fadeIn, int stay, int fadeOut) {
        player.sendActionBar(message, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendToast(Player player, String title, String body) {
        player.sendToast(title, body);
    }

    @Override
    public void kick(Player player, String reason, String disconnectScreenMessage) {
        player.kick(reason, disconnectScreenMessage);
    }

    @Override
    public void transfer(Player player, ICloudServer server) {
        TransferPacket packet = new TransferPacket();
        packet.setServerAddress(server.name());
        packet.setServerPort(server.data().port());
        packet.setReloadWorld(true);
        packet.setGatheringsConfiguration(null);
        player.sendPacket(packet);
    }

    @Override
    public Optional<Player> find(String nameOrXuid) {
        Player byName = Server.getInstance().getPlayerExact(nameOrXuid);
        if (byName != null) return Optional.of(byName);
        return Server.getInstance().getOnlinePlayers().values().stream().filter(p -> p.getXUID().equals(nameOrXuid)).findFirst();
    }

    @Override
    public Optional<Player> find(UUID uuid) {
        return Server.getInstance().getPlayer(uuid);
    }
}