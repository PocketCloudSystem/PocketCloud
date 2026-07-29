package de.pocketcloud.bridge.network.packet.handler;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.language.LanguageKey;
import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.cache.NotificationListCache;
import de.pocketcloud.bridge.cache.RandomCache;
import de.pocketcloud.bridge.cache.WhitelistCache;
import de.pocketcloud.bridge.component.*;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.network.packet.impl.*;
import de.pocketcloud.shared.component.software.ServerSoftware;
import de.pocketcloud.shared.event.player.PlayerJoinFailedEvent;
import de.pocketcloud.shared.event.player.PlayerKickedEvent;
import de.pocketcloud.shared.event.server.ServerCrashedEvent;
import de.pocketcloud.shared.event.server.ServerStartFailedEvent;
import de.pocketcloud.shared.event.server.ServerStopTimedOutEvent;
import de.pocketcloud.shared.event.server.ServerTimedOutEvent;
import de.pocketcloud.shared.network.packet.type.NotificationType;
import de.pocketcloud.shared.network.packet.type.ServerDisconnectReason;
import de.pocketcloud.shared.network.packet.type.TextType;
import de.pocketcloud.shared.sync.SyncType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NormalPacketHandler implements PacketListener {

    @PacketHandler(CloudNotificationPacket.class)
    public void handle(CloudNotificationPacket packet) {
        if (packet.getNotificationType() == NotificationType.SERVER_CRASHED) {
            CloudAPI.instance().events().call(new ServerCrashedEvent(CloudAPI.instance().servers().get(packet.getArgs().get("server").toString()).orElse(null)));
        } else if (packet.getNotificationType() == NotificationType.SERVER_TIMED_OUT) {
            CloudAPI.instance().events().call(new ServerTimedOutEvent(CloudAPI.instance().servers().get(packet.getArgs().get("server").toString()).orElse(null)));
        } else if (packet.getNotificationType() == NotificationType.SERVER_STOP_TIMED_OUT) {
            CloudAPI.instance().events().call(new ServerStopTimedOutEvent(CloudAPI.instance().servers().get(packet.getArgs().get("server").toString()).orElse(null)));
        } else if (packet.getNotificationType() == NotificationType.SERVER_START_FAILED) {
            CloudAPI.instance().events().call(new ServerStartFailedEvent(CloudAPI.instance().servers().get(packet.getArgs().get("server").toString()).orElse(null), packet.getArgs().get("reason").toString()));
        } else if (packet.getNotificationType() == NotificationType.PLAYER_KICKED) {
            CloudAPI.instance().events().call(new PlayerKickedEvent(CloudAPI.instance().players().get(packet.getArgs().get("player").toString()).orElse(null), CloudAPI.instance().servers().get(packet.getArgs().get("server").toString()).orElse(null), packet.getArgs().get("reason").toString()));
        } else if (packet.getNotificationType() == NotificationType.PLAYER_JOIN_FAILED) {
            CloudAPI.instance().events().call(new PlayerJoinFailedEvent(CloudAPI.instance().players().get(packet.getArgs().get("player").toString()).orElse(null), CloudAPI.instance().servers().get(packet.getArgs().get("server").toString()).orElse(null), packet.getArgs().get("reason").toString()));
        }

        LanguageKey langKey = de.pocketcloud.shared.notification.NotificationHelper.toLangKey(packet.getNotificationType());
        for (Map.Entry<String, Boolean> entry : LocalCache.get(NotificationListCache.class).getAll().entrySet()) {
            if (entry.getValue()) {
                CloudAPI.instance().playerExecutor().sendMessage(entry.getKey(), langKey.translate(packet.getArgs()));
            }
        }
    }

    @PacketHandler(ConsoleLogPacket.class)
    public void handle(ConsoleLogPacket packet) {
        CloudBridge.instance().logger().log(de.pocketcloud.shared.logging.CloudLogLevelHelper.toLogLevel(packet.getLogType()), packet.getMessage());
    }

    @PacketHandler(DisconnectPacket.class)
    public void handle(DisconnectPacket packet) {
        if (packet.getReason() == ServerDisconnectReason.SERVER_SHUTDOWN) {
            CloudBridge.instance().logger().warn("Server shutdown was ordered by the cloud, shutting down...");
        } else {
            CloudBridge.instance().logger().warn("Cloud was stopped, shutting down...");
        }

        CloudBridge.instance().shutdown();
    }

    @PacketHandler(KeepAlivePacket.class)
    public void handle(KeepAlivePacket packet) {
        LocalCache.get(RandomCache.class).add(RandomCache.KEY_LAST_KEEP_ALIVE, System.currentTimeMillis());
        CloudBridge.instance().constructKeepAlive().sendPacket();
    }

    @PacketHandler(PlayerKickPacket.class)
    public void handle(PlayerKickPacket packet) {
        CloudBridge.instance().playerExecutor().kick(packet.getPlayer(), packet.getReason(), packet.getDisconnectScreenMessage());
    }

    @PacketHandler(PlayerTextPacket.class)
    public void handle(PlayerTextPacket packet) {
        String player = packet.getPlayer();
        TextType type = packet.getType();
        switch (type) {
            case MESSAGE -> CloudBridge.instance().playerExecutor().sendMessage(player, packet.getBody());
            case POPUP -> CloudBridge.instance().playerExecutor().sendPopup(player, packet.getTitle(), packet.getBody());
            case TIP -> CloudBridge.instance().playerExecutor().sendTip(player, packet.getBody());
            case TITLE -> CloudBridge.instance().playerExecutor().sendTitle(player, packet.getTitle(), packet.getBody(), packet.getFadeIn(), packet.getStay(), packet.getFadeOut());
            case ACTION_BAR -> CloudBridge.instance().playerExecutor().sendActionbarMessage(player, packet.getBody(), packet.getFadeIn(), packet.getStay(), packet.getFadeOut());
            case TOAST -> CloudBridge.instance().playerExecutor().sendToast(player, packet.getTitle(), packet.getBody());
        }
    }

    @PacketHandler(SyncPacket.class)
    public void handle(SyncPacket packet) {
        SyncType type = packet.getSyncType();
        IPacketData remainingData = packet.getRemainingData();

        if (type == SyncType.SERVERS) {
            for (CloudServer server : handleBulkSync(remainingData, CloudServer.class)) {
                CloudBridge.instance().servers().add(server);
            }
        } else if (type == SyncType.SERVER) {
            CloudServer server = MapperUtils.fromMap(remainingData.readMap(), CloudServer.class);
            boolean removal = remainingData.readBool();
            if (removal) CloudBridge.instance().servers().remove(server);
            else CloudBridge.instance().servers().add(server);
        } else if (type == SyncType.SERVER_STORAGE) {
            CloudAPI.instance().servers().get(UUID.fromString(remainingData.readString()))
                            .ifPresent(s -> s.storage().syncIn(remainingData.readMap()));
        } else if (type == SyncType.TEMPLATES) {
            for (Template template : handleBulkSync(remainingData, Template.class)) {
                CloudBridge.instance().templates().add(template);
            }
        } else if (type == SyncType.TEMPLATE) {
            Template template = MapperUtils.fromMap(remainingData.readMap(), Template.class);
            boolean removal = remainingData.readBool();
            if (removal) CloudBridge.instance().templates().remove(template);
            else CloudBridge.instance().templates().add(template);
        } else if (type == SyncType.SERVER_GROUPS) {
            for (ServerGroup group : handleBulkSync(remainingData, ServerGroup.class)) {
                CloudBridge.instance().serverGroups().add(group);
            }
        } else if (type == SyncType.SERVER_GROUP) {
            ServerGroup group = MapperUtils.fromMap(remainingData.readMap(), ServerGroup.class);
            boolean removal = remainingData.readBool();
            if (removal) CloudBridge.instance().serverGroups().remove(group);
            else CloudBridge.instance().serverGroups().add(group);
        } else if (type == SyncType.PLAYERS) {
            for (CloudPlayer player : handleBulkSync(remainingData, CloudPlayer.class)) {
                CloudBridge.instance().players().add(player);
            }
        } else if (type == SyncType.PLAYER) {
            CloudPlayer player = MapperUtils.fromMap(remainingData.readMap(), CloudPlayer.class);
            boolean removal = remainingData.readBool();
            if (removal) CloudBridge.instance().players().remove(player);
            else CloudBridge.instance().players().add(player);
        } else if (type == SyncType.LANGUAGE) {
            CloudBridge.instance().language().setCurrentLanguage(new MinimalLanguage(
                    remainingData.readString(),
                    remainingData.readMap(String.class)
            ));
        } else if (type == SyncType.WHITELIST) {
            LocalCache.get(WhitelistCache.class).syncIn(remainingData.readMap(Boolean.class));
        } else if (type == SyncType.NOTIFICATION_LIST) {
            LocalCache.get(NotificationListCache.class).syncIn(remainingData.readMap(Boolean.class));
        } else if (type == SyncType.SOFTWARES) {
            for (ServerSoftware serverSoftware : handleBulkSync(remainingData, ServerSoftware.class)) {
                CloudBridge.instance().softwares().register(serverSoftware, true);
            }
        }
    }

    private <T> ArrayList<T> handleBulkSync(IPacketData data, Class<T> clazz) {
        List<Map<String, Object>> raw = data.readArray(new TypeToken<Map<String, Object>>() {});
        return new ArrayList<>(raw.stream().map(m -> MapperUtils.fromMap(m, clazz)).toList());
    }
}