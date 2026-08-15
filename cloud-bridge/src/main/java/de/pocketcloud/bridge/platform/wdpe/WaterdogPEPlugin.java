package de.pocketcloud.bridge.platform.wdpe;

import de.pocketcloud.api.CloudAPIHolder;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.adapter.NativePlayerAdapter;
import de.pocketcloud.bridge.api.IPlatformPlugin;
import de.pocketcloud.bridge.config.LocalServerConfig;
import de.pocketcloud.bridge.platform.wdpe.adapter.WaterdogPEPlayerAdapter;
import de.pocketcloud.bridge.platform.wdpe.handler.JoinAndFallbackHandler;
import de.pocketcloud.bridge.platform.wdpe.handler.ProxyPacketHandler;
import de.pocketcloud.bridge.platform.wdpe.listener.PlayerListener;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.InitialServerDeterminedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.ServerTransferEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.plugin.Plugin;
import dev.waterdog.waterdogpe.utils.config.YamlConfig;

import java.util.Map;

public final class WaterdogPEPlugin extends Plugin implements IPlatformPlugin {

    @Override
    public void onStartup() {
        CloudAPIHolder.setInstance(new CloudBridge(this, craftPlatformLogger(), fetchEnvironmentConfig(), buildNativePlayerAdapter()));
        CloudBridge.instance().packets().registerPacketListener(new ProxyPacketHandler());
    }

    @Override
    public void onEnable() {
        getProxy().setJoinHandler(new JoinAndFallbackHandler());
        getProxy().setReconnectHandler(new JoinAndFallbackHandler());
        getProxy().getEventManager().subscribe(PlayerLoginEvent.class, PlayerListener::handle);
        getProxy().getEventManager().subscribe(PlayerDisconnectedEvent.class, PlayerListener::handle);
        getProxy().getEventManager().subscribe(ServerTransferEvent.class, PlayerListener::handle);
        getProxy().getEventManager().subscribe(InitialServerDeterminedEvent.class, PlayerListener::handle);
    }

    @Override
    public void onDisable() {
        CloudBridge.instance().shutdown();
    }

    @Override
    public void onVerification() {}

    @Override
    public void startTask(Runnable runnable, int period) {
        getProxy().getScheduler().scheduleRepeating(runnable, period);
    }

    @Override
    public ILogger craftPlatformLogger() {
        return new WaterdogPELogger(getLogger());
    }

    @SuppressWarnings("unchecked")
    @Override
    public LocalServerConfig fetchEnvironmentConfig() {
        Map<String, Object> environmentSettings = (Map<String, Object>) new YamlConfig(ProxyServer.getInstance().getDataPath().toString() + "/config.yml").get("environment-settings");
        return LocalServerConfig.fromMap(environmentSettings);
    }

    public NativePlayerAdapter<ProxiedPlayer> buildNativePlayerAdapter() {
        return new WaterdogPEPlayerAdapter();
    }

    @Override
    public void shutdownServer() {
        getProxy().shutdown();
    }

    @Override
    public double tps() {
        return -1;
    }

    @Override
    public double avgTps() {
        return -1;
    }

    @Override
    public int currentPlayers() {
        return getProxy().getPlayers().size();
    }

    @Override
    public int maxPlayers() {
        return getProxy().getConfiguration().getMaxPlayerCount();
    }
}