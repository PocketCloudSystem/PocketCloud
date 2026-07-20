package de.pocketcloud.bridge.platform.wdpe;

import de.pocketcloud.api.CloudAPIHolder;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.adapter.NativePlayerAdapter;
import de.pocketcloud.bridge.api.IPlatformPlugin;
import de.pocketcloud.bridge.config.LocalServerConfig;
import de.pocketcloud.bridge.platform.wdpe.handler.JoinAndFallbackHandler;
import de.pocketcloud.bridge.platform.wdpe.listener.PlayerListener;
import de.pocketcloud.bridge.task.ChangeStatusTask;
import de.pocketcloud.bridge.task.RequestTimeoutTask;
import de.pocketcloud.bridge.task.ServerTimeoutTask;
import de.pocketcloud.bridge.task.UpdatePerformanceStatsTask;
import dev.waterdog.waterdogpe.event.defaults.InitialServerDeterminedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.ServerTransferEvent;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.plugin.Plugin;
import org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket;

import java.util.Optional;
import java.util.UUID;

public final class WaterdogPEPlugin extends Plugin implements IPlatformPlugin {

    @Override
    public void onStartup() {
        CloudAPIHolder.setInstance(new CloudBridge(this, craftPlatformLogger(), fetchEnvironmentConfig(), buildNativePlayerAdapter()));
    }

    @Override
    public void onEnable() {
        getProxy().setJoinHandler(new JoinAndFallbackHandler());
        getProxy().setForcedHostHandler(new JoinAndFallbackHandler());
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
    public void startTasks() {
        getProxy().getScheduler().scheduleRepeating(new ChangeStatusTask(), 10);
        getProxy().getScheduler().scheduleRepeating(new RequestTimeoutTask(), 10);
        getProxy().getScheduler().scheduleRepeating(new ServerTimeoutTask(), 10);
        getProxy().getScheduler().scheduleRepeating(new UpdatePerformanceStatsTask(), 10);
    }

    @Override
    public ILogger craftPlatformLogger() {
        return new WaterdogPELogger(getLogger());
    }

    public LocalServerConfig fetchEnvironmentConfig() {
        return null;
    }

    public NativePlayerAdapter<ProxiedPlayer> buildNativePlayerAdapter() {
        return new NativePlayerAdapter<>() {

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
                ServerInfo info = getProxy().getServerInfo(server.name());
                if (info != null) player.redirectServer(info);
            }

            @Override
            public Optional<ProxiedPlayer> find(String nameOrXuid) {
                ProxiedPlayer byName = getProxy().getPlayer(nameOrXuid);
                if (byName != null) return Optional.of(byName);
                return getProxy().getPlayers().values().stream().filter(p -> p.getXuid().equals(nameOrXuid)).findFirst();
            }

            @Override
            public Optional<ProxiedPlayer> find(UUID uuid) {
                return Optional.ofNullable(getProxy().getPlayer(uuid));
            }
        };
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