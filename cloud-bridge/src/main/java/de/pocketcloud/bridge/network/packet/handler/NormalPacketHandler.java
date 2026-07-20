package de.pocketcloud.bridge.network.packet.handler;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.command.sender.PNXCloudCommandSender;
import de.pocketcloud.bridge.command.sender.WDPECloudCommandSender;
import de.pocketcloud.bridge.component.CloudPlayer;
import de.pocketcloud.bridge.component.CloudServer;
import de.pocketcloud.bridge.component.ServerGroup;
import de.pocketcloud.bridge.component.Template;
import de.pocketcloud.bridge.platform.pnx.PowerNukkitXPlugin;
import de.pocketcloud.bridge.platform.wdpe.WaterdogPEPlugin;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.network.packet.impl.request.client.CommandExecuteRequestPacket;
import de.pocketcloud.network.packet.impl.response.client.CommandExecuteResponsePacket;
import de.pocketcloud.shared.network.packet.type.ServerCommandExecutionResult;
import de.pocketcloud.shared.sync.SyncType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NormalPacketHandler implements PacketListener {

    @PacketHandler(CommandExecuteRequestPacket.class)
    public void handle(CommandExecuteRequestPacket packet) {
        if (CloudBridge.instance().platformPlugin() instanceof WaterdogPEPlugin wdpe) {
            WDPECloudCommandSender wdpeSender = new WDPECloudCommandSender(packet.getId());
            wdpe.getProxy().dispatchCommand(wdpeSender, packet.getCommandLine());
            packet.sendResponse(CommandExecuteResponsePacket.create(new ServerCommandExecutionResult(packet.getId(), packet.getCommandLine(), wdpeSender.getCachedMessages())));
        } else if (CloudBridge.instance().platformPlugin() instanceof PowerNukkitXPlugin pnx) {
            PNXCloudCommandSender pnxSender = new PNXCloudCommandSender(packet.getId());
            pnx.getServer().executeCommand(pnxSender, packet.getCommandLine());
            packet.sendResponse(CommandExecuteResponsePacket.create(new ServerCommandExecutionResult(packet.getId(), packet.getCommandLine(), pnxSender.getCachedMessages())));
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
            CloudBridge.instance().servers().add(server);
        } else if (type == SyncType.SERVER_STORAGE) {
            CloudAPI.instance().servers().get(UUID.fromString(remainingData.readString()))
                            .ifPresent(s -> s.storage().syncIn(remainingData.readMap()));
        } else if (type == SyncType.TEMPLATES) {
            for (Template template : handleBulkSync(remainingData, Template.class)) {
                CloudBridge.instance().templates().add(template);
            }
        } else if (type == SyncType.TEMPLATE) {
            Template template = MapperUtils.fromMap(remainingData.readMap(), Template.class);
            CloudBridge.instance().templates().add(template);
        } else if (type == SyncType.SERVER_GROUPS) {
            for (ServerGroup group : handleBulkSync(remainingData, ServerGroup.class)) {
                CloudBridge.instance().serverGroups().add(group);
            }
        } else if (type == SyncType.SERVER_GROUP) {
            ServerGroup group = MapperUtils.fromMap(remainingData.readMap(), ServerGroup.class);
            CloudBridge.instance().serverGroups().add(group);
        } else if (type == SyncType.PLAYERS) {
            for (CloudPlayer player : handleBulkSync(remainingData, CloudPlayer.class)) {
                CloudBridge.instance().players().add(player);
            }
        } else if (type == SyncType.PLAYER) {
            CloudPlayer player = MapperUtils.fromMap(remainingData.readMap(), CloudPlayer.class);
            CloudBridge.instance().players().add(player);
        }
    }

    private <T> ArrayList<T> handleBulkSync(IPacketData data, Class<T> clazz) {
        List<Map<String, Object>> rawTemplates = data.readArray(new TypeToken<Map<String, Object>>() {});
        return new ArrayList<>(rawTemplates.stream().map(m -> MapperUtils.fromMap(m, clazz)).toList());
    }
}