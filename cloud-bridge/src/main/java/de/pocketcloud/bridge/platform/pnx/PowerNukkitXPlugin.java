package de.pocketcloud.bridge.platform.pnx;

import de.pocketcloud.api.CloudAPIHolder;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.adapter.NativePlayerAdapter;
import de.pocketcloud.bridge.api.IPlatformPlugin;
import de.pocketcloud.bridge.config.LocalServerConfig;
import de.pocketcloud.bridge.platform.pnx.adapter.PowerNukkitXPlayerAdapter;
import de.pocketcloud.bridge.platform.pnx.auth.WaterdogProxyAuthProvider;
import de.pocketcloud.bridge.platform.pnx.command.CloudNotifyCommand;
import de.pocketcloud.bridge.platform.pnx.command.TransferCommand;
import de.pocketcloud.bridge.platform.pnx.handler.ServerPacketHandler;
import de.pocketcloud.bridge.platform.pnx.listener.PlayerListener;
import org.powernukkitx.Player;
import org.powernukkitx.Server;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.utils.Config;

import java.util.List;
import java.util.Map;

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
    public void onVerification() {
        getServer().getCommandMap().registerAll("cloudBridge", List.of(
                new TransferCommand(), new CloudNotifyCommand()
        ));

        getServer().setProxyAuthProvider(new WaterdogProxyAuthProvider());
        getServer().checkLoginTime = false;
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
        return new PowerNukkitXPlayerAdapter();
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