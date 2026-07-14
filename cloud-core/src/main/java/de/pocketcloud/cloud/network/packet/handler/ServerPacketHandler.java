package de.pocketcloud.cloud.network.packet.handler;

import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.server.ServerPostVerificationEvent;
import de.pocketcloud.cloud.event.impl.server.ServerVerifyEvent;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.server.CloudServersHandler;
import de.pocketcloud.network.packet.impl.*;
import de.pocketcloud.network.packet.impl.request.ServerHandshakeRequestPacket;
import de.pocketcloud.network.packet.impl.request.ServerSaveRequestPacket;
import de.pocketcloud.network.packet.impl.request.ServerStartRequestPacket;
import de.pocketcloud.network.packet.impl.request.ServerStopRequestPacket;
import de.pocketcloud.network.packet.impl.response.ServerHandshakeResponsePacket;
import de.pocketcloud.network.packet.impl.response.ServerSaveResponsePacket;
import de.pocketcloud.network.packet.impl.response.ServerStartResponsePacket;
import de.pocketcloud.network.packet.impl.response.ServerStopResponsePacket;
import de.pocketcloud.network.packet.type.ActionFailureReason;

import java.time.Duration;
import java.time.Instant;

public final class ServerPacketHandler implements PacketListener {

    @PacketHandler({ServerHandshakeRequestPacket.class})
    public void handle(ServerHandshakeRequestPacket packet, ServerClient sender) {
        var server = PocketCloud.instance().servers().get(packet.getServerName()).orElse(null);
        if (server == null) return;
        if (PocketCloud.instance().clients().getServer(sender).isEmpty()) {
            var ev = new ServerVerifyEvent(server);
            ev.call();
            if (ev.isCancelled()) {
                server.logger().warn("Denied server handshake request by event §8(§b{}§8)", sender.address());
                return;
            }

            float elapsed = Duration.between(server.startTime(), Instant.now()).toSeconds();

            PocketCloud.instance().clients().add(server, sender);
            CloudLogger.get().success("The server §b{} §rhas §aconnected §rto the cloud. §8(§rTook §b{}§rs§8)", server.name(), elapsed);
            server.data().maxPlayers(packet.getMaxPlayers());
            server.data().processId(packet.getProcessId());
            server.verifiedTime(Instant.now());
            server.verificationStatus(VerificationStatus.VERIFIED);
            server.addToProxies();
            server.sync();
            new ServerPostVerificationEvent(server).call();
            packet.sendResponse(new ServerHandshakeResponsePacket(VerificationStatus.VERIFIED), sender);
            server.setStatus(ServerStatus.ONLINE);
        } else {
            server.logger().warn("Denied server handshake request, duplicate server... §8(§b{}§r§8)", sender.address());
            CloudLogger.get().warn("Denied server handshake request from §b{} §8(§b{}§8)§r, duplicate server...", packet.getServerName(), sender.address());
            packet.sendResponse(new ServerHandshakeResponsePacket(VerificationStatus.DENIED), sender);
        }
    }

    @PacketHandler({DisconnectPacket.class})
    public void handle(DisconnectPacket packet, ServerClient sender) {
        CloudServersHandler.handleDisconnect(sender.server(), packet.getReason());
    }

    @PacketHandler({KeepAlivePacket.class})
    public void handle(KeepAlivePacket packet, ServerClient sender) {
        var server = sender.server();
        server.lastKeepAlive(System.currentTimeMillis() / 1000L);
        server.data().setPerformanceStats(packet.getTps(), packet.getAvgTps(), packet.getMemoryUsage(), packet.getMemoryPeak(), packet.getMemoryLimit(), packet.getCpuUsage());
    }

    @PacketHandler({ServerSaveRequestPacket.class})
    public void handle(ServerSaveRequestPacket packet, ServerClient sender) {
        var cloudServer = PocketCloud.instance().servers().get(packet.getServer());
        if (cloudServer.isEmpty()) {
            packet.sendResponse(ServerSaveResponsePacket.create(ActionFailureReason.SERVER_NOT_FOUND), sender);
            return;
        }

        PocketCloud.instance().servers().save(cloudServer.get())
            .thenSuccess(_ -> packet.sendResponse(ServerSaveResponsePacket.create(ActionFailureReason.NONE), sender))
            .failure(_ -> packet.sendResponse(ServerSaveResponsePacket.create(ActionFailureReason.REQUEST_TIMEOUT), sender));
    }

    @PacketHandler({ServerStartRequestPacket.class})
    public void handle(ServerStartRequestPacket packet, ServerClient sender) {
        var tmpl = PocketCloud.instance().templates().get(packet.getTemplateName()).orElse(null);
        if (tmpl != null) {
            if (PocketCloud.instance().servers().query(ServerSearchQuery.create().ofTemplate(tmpl)).size() < tmpl.settings().maxServerCount()) {
                PocketCloud.instance().servers().start(tmpl, packet.getCount());
                //TODO append started server names to resp. packet
                packet.sendResponse(ServerStartResponsePacket.create(ActionFailureReason.NONE), sender);
            } else {
                packet.sendResponse(ServerStartResponsePacket.create(ActionFailureReason.MAX_SERVERS_REACHED), sender);
            }
        } else {
            packet.sendResponse(ServerStartResponsePacket.create(ActionFailureReason.TEMPLATE_NOT_FOUND), sender);
        }
    }

    @PacketHandler({ServerStopRequestPacket.class})
    public void handle(ServerStopRequestPacket packet, ServerClient sender) {
        PocketCloud.instance().servers().stop(packet.getServer(), packet.isForcefully()).thenSuccess(c -> {
            //TODO: append affected servers to resp. packet
            if (c.isEmpty()) {
                packet.sendResponse(ServerStopResponsePacket.create(ActionFailureReason.SERVER_NOT_FOUND), sender);
            } else {
                packet.sendResponse(ServerStopResponsePacket.create(ActionFailureReason.NONE), sender);
            }
        });
    }

    @PacketHandler({CloudSyncServerStoragePacket.class})
    public void handle(CloudSyncServerStoragePacket packet, ServerClient sender) {
        var server = sender.server();
        if (server != null) {
            server.storage().clear();
            server.storage().setAll(packet.getData());
        }
    }

    @PacketHandler({ServerChangeStatusPacket.class})
    public void handle(ServerChangeStatusPacket packet, ServerClient sender) {
        PocketCloud.instance().servers().get(packet.getServerUuid()).ifPresent(server -> server.setStatus(packet.getStatus()));
    }
}
