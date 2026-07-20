package de.pocketcloud.cloud.network.packet.handler;

import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.sync.SyncType;

import java.util.Map;

public final class SyncPacketHandler implements PacketListener {

    @PacketHandler(SyncPacket.class)
    public void handle(SyncPacket packet, ServerClient sender) {
        SyncType syncType = packet.getSyncType();
        IPacketData data = packet.getRemainingData();
        Map<String, Object> customProperties = packet.getCustomProperties();

        if (syncType == SyncType.SERVER_STATUS) {
            handleServerStatus(data, sender, customProperties);
        } else if (syncType == SyncType.SERVER_STORAGE) {
            handleServerStorage(data, sender, customProperties);
        } else if (syncType == SyncType.PLAYER_NOTIFICATION_STATE) {
            handlePlayerUpdateNotificationState(data, sender, customProperties);
        } else if (syncType == SyncType.PLAYER_WHITELIST_STATE) {
            handlePlayerUpdateWhitelistState(data, sender, customProperties);
        }
    }

    public void handleServerStatus(IPacketData data, ServerClient sender, Map<String, Object> customProperties) {

        sender.server().setStatus(ServerStatus.valueOf(data.toString()));
    }

    public void handleServerStorage(IPacketData data, ServerClient sender, Map<String, Object> customProperties) {
        Map<String, Object> storage = data.readMap();
        sender.server().storage().syncIn(storage);
    }

    public void handlePlayerUpdateNotificationState(IPacketData data, ServerClient sender, Map<String, Object> customProperties) {
        String playerName = data.readString();
        boolean value = data.readBool();

        if (value) CloudProvider.current().enablePlayerNotifications(playerName);
        else CloudProvider.current().disablePlayerNotifications(playerName);
    }

    public void handlePlayerUpdateWhitelistState(IPacketData data, ServerClient sender, Map<String, Object> customProperties) {
        String playerName = data.readString();
        boolean value = data.readBool();

        if (value) CloudProvider.current().addToWhitelist(playerName);
        else CloudProvider.current().removeFromWhitelist(playerName);
    }
}