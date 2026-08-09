package de.pocketcloud.bridge.platform.pnx;

import de.pocketcloud.api.CloudAPIHolder;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.adapter.NativePlayerAdapter;
import de.pocketcloud.bridge.api.IPlatformPlugin;
import de.pocketcloud.bridge.config.LocalServerConfig;
import de.pocketcloud.bridge.platform.pnx.command.CloudNotifyCommand;
import de.pocketcloud.bridge.platform.pnx.command.TransferCommand;
import de.pocketcloud.bridge.platform.pnx.handler.ServerPacketHandler;
import de.pocketcloud.bridge.platform.pnx.listener.PlayerListener;
import de.pocketcloud.network.packet.impl.PlayerTransferPacket;
import org.powernukkitx.Player;
import org.powernukkitx.Server;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.utils.Config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PowerNukkitXPlugin extends PluginBase implements IPlatformPlugin {

    @Override
    public void onLoad() {
        CloudAPIHolder.setInstance(new CloudBridge(this, craftPlatformLogger(), fetchEnvironmentConfig(), buildNativePlayerAdapter()));
        CloudBridge.instance().packets().registerPacketListener(new ServerPacketHandler());
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
    }

    @Override
    public void registerCommands() {
        getServer().getCommandMap().registerAll("cloudBridge", List.of(
                new TransferCommand(), new CloudNotifyCommand()
        ));
    }

    @Override
    public void onDisable() {
        CloudBridge.instance().shutdown();
    }

    @Override
    public void startTask(Runnable runnable, int period) {
        getServer().getScheduler().scheduleRepeatingTask(runnable, period);
    }

    @Override
    public ILogger craftPlatformLogger() {
        return new PowerNukkitXLogger(getLogger());
    }

    @Override
    public LocalServerConfig fetchEnvironmentConfig() {
        Map<String, Object> environmentSettings = new Config(Server.getInstance().getDataPath() + "/pnx_cloud.yml").getAll();
        return LocalServerConfig.fromMap(environmentSettings);
    }

    public NativePlayerAdapter<Player> buildNativePlayerAdapter() {
        return new NativePlayerAdapter<>() {

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
                PlayerTransferPacket.create(player.getName(), server.name()).sendPacket();
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
        };
    }

    @Override
    public void shutdownServer() {
        Server.getInstance().shutdown();
    }

    @Override
    public double tps() {
        return getServer().getTicksPerSecond();
    }

    @Override
    public double avgTps() {
        return getServer().getTicksPerSecondAverage();
    }

    @Override
    public int currentPlayers() {
        return getServer().getOnlinePlayers().size();
    }

    @Override
    public int maxPlayers() {
        return getServer().getMaxPlayers();
    }
}