package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.load.Loadable;
import de.pocketcloud.cloud.network.packet.impl.*;
import de.pocketcloud.cloud.network.packet.impl.request.*;
import de.pocketcloud.cloud.network.packet.impl.request.client.CommandExecuteRequestPacket;
import de.pocketcloud.cloud.network.packet.impl.response.*;
import de.pocketcloud.cloud.network.packet.impl.response.client.CommandExecuteResponsePacket;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Getter
@Accessors(fluent = true)
public final class PacketPool implements Loadable {

    @Getter
    private static PacketPool instance;
    
    private final Map<String, Supplier<CloudPacket>> packets = new ConcurrentHashMap<>();
    
    public PacketPool() {
        instance = this;
    }

    public void load() {
        register(CommandExecuteRequestPacket.class, CommandExecuteRequestPacket::new);
        register(PlayerNotificationCheckRequestPacket.class, PlayerNotificationCheckRequestPacket::new);
        register(PlayerWhitelistCheckRequestPacket.class, PlayerWhitelistCheckRequestPacket::new);
        register(ServerHandshakeRequestPacket.class, ServerHandshakeRequestPacket::new);
        register(ServerSaveRequestPacket.class, ServerSaveRequestPacket::new);
        register(ServerStartRequestPacket.class, ServerStartRequestPacket::new);
        register(ServerStopRequestPacket.class, ServerStopRequestPacket::new);

        register(CommandExecuteResponsePacket.class, CommandExecuteResponsePacket::new);
        register(PlayerNotificationCheckResponsePacket.class, PlayerNotificationCheckResponsePacket::new);
        register(PlayerWhitelistCheckResponsePacket.class, PlayerWhitelistCheckResponsePacket::new);
        register(ServerHandshakeResponsePacket.class, ServerHandshakeResponsePacket::new);
        register(ServerSaveResponsePacket.class, ServerSaveResponsePacket::new);
        register(ServerStartResponsePacket.class, ServerStartResponsePacket::new);
        register(ServerStopResponsePacket.class, ServerStopResponsePacket::new);

        register(BulkSyncPacket.class, BulkSyncPacket::new);
        register(CloudNotificationPacket.class, CloudNotificationPacket::new);
        register(CloudSyncServerStoragePacket.class, CloudSyncServerStoragePacket::new);
        register(ConsoleLogPacket.class, ConsoleLogPacket::new);
        register(DisconnectPacket.class, DisconnectPacket::new);
        register(KeepAlivePacket.class, KeepAlivePacket::new);
        register(LanguageSyncPacket.class, LanguageSyncPacket::new);
        register(LibrarySyncPacket.class, LibrarySyncPacket::new);
        register(MaintenanceListSyncPacket.class, MaintenanceListSyncPacket::new);
        register(ModuleSyncPacket.class, ModuleSyncPacket::new);
        register(NotificationListSyncPacket.class, NotificationListSyncPacket::new);
        register(PlayerConnectPacket.class, PlayerConnectPacket::new);
        register(PlayerDisconnectPacket.class, PlayerDisconnectPacket::new);
        register(PlayerKickPacket.class, PlayerKickPacket::new);
        register(PlayerSwitchServerPacket.class, PlayerSwitchServerPacket::new);
        register(PlayerSyncPacket.class, PlayerSyncPacket::new);
        register(PlayerTextPacket.class, PlayerTextPacket::new);
        register(PlayerTransferPacket.class, PlayerTransferPacket::new);
        register(PlayerUpdateNotificationStatePacket.class, PlayerUpdateNotificationStatePacket::new);
        register(ProxyRegisterServerPacket.class, ProxyRegisterServerPacket::new);
        register(ProxyUnregisterServerPacket.class, ProxyUnregisterServerPacket::new);
        register(ServerChangeStatusPacket.class, ServerChangeStatusPacket::new);
        register(ServerGroupSyncPacket.class, ServerGroupSyncPacket::new);
        register(ServerSyncPacket.class, ServerSyncPacket::new);
        register(TemplateSyncPacket.class, TemplateSyncPacket::new);
    }

    @Override
    public void unload() {
        packets.clear();
    }

    public void register(Class<? extends CloudPacket> packetClass, Supplier<CloudPacket> supplier) {
        synchronized (packets) {
            String packetName = packetClass.getSimpleName();
            packets.put(packetName, supplier);
        }
    }

    public CloudPacket get(String packetName) {
        Supplier<CloudPacket> supplier = packets.get(packetName);
        return supplier != null ? supplier.get() : null;
    }

    public Map<String, Supplier<CloudPacket>> getAll() {
        return new HashMap<>(packets);
    }
}