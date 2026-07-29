package de.pocketcloud.cloud.network.packet.handler;

import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.cloud.PocketCloud;
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

        if (syncType == SyncType.SERVER_STATUS) {
            handleServerStatus(data, sender);
        } else if (syncType == SyncType.SERVER_STORAGE) {
            handleServerStorage(data, sender);
        } else if (syncType == SyncType.PLAYER_NOTIFICATION_STATE) {
            handlePlayerUpdateNotificationState(data, sender);
        } else if (syncType == SyncType.PLAYER_WHITELIST_STATE) {
            handlePlayerUpdateWhitelistState(data, sender);
        }
    }

    public void handleServerStatus(IPacketData data, ServerClient sender) {
        PocketCloud.instance().servers().get(data.readString()).ifPresent(server -> server.status(ServerStatus.valueOf(data.readString())));
    }

    public void handleServerStorage(IPacketData data, ServerClient sender) {
        Map<String, Object> storage = data.readMap();
        sender.server().storage().syncIn(storage);
    }

    public void handlePlayerUpdateNotificationState(IPacketData data, ServerClient sender) {
        String playerName = data.readString();
        boolean value = data.readBool();

        if (value) CloudProvider.current().enablePlayerNotifications(playerName);
        else CloudProvider.current().disablePlayerNotifications(playerName);
    }

    public void handlePlayerUpdateWhitelistState(IPacketData data, ServerClient sender) {
        String playerName = data.readString();
        boolean value = data.readBool();

        if (value) CloudProvider.current().addToWhitelist(playerName);
        else CloudProvider.current().removeFromWhitelist(playerName);
    }
}