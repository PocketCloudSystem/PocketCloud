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
import de.pocketcloud.common.config.Config;
import de.pocketcloud.common.config.exception.UnsupportedFileExtensionException;
import de.pocketcloud.common.config.type.EnvironmentConfigType;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.InitialServerDeterminedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.ServerTransferEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.plugin.Plugin;

import java.io.IOException;
import java.util.Map;

public final class WaterdogPEPlugin extends Plugin implements IPlatformPlugin {

    @Override
    public void onStartup() {
        try {
            CloudAPIHolder.setInstance(new CloudBridge(this, craftPlatformLogger(), fetchEnvironmentConfig(), buildNativePlayerAdapter()));
        } catch (UnsupportedFileExtensionException | IOException e) {
            getLogger().error("Failed to load environment settings, shutting down...", e);
            getProxy().shutdown();
            return;
        }

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
    public LocalServerConfig fetchEnvironmentConfig() throws UnsupportedFileExtensionException, IOException {
        Map<String, Object> environmentSettings = new Config(ProxyServer.getInstance().getDataPath().resolve(".env"), new EnvironmentConfigType()).getAll();
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