package de.pocketcloud.cloud.network.packet.handler;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.server.ServerRequestVerificationEvent;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.CloudServersHandler;
import de.pocketcloud.common.util.NumberUtils;
import de.pocketcloud.network.packet.impl.DisconnectPacket;
import de.pocketcloud.network.packet.impl.KeepAlivePacket;
import de.pocketcloud.network.packet.impl.request.ServerHandshakeRequestPacket;
import de.pocketcloud.network.packet.impl.request.ServerSaveRequestPacket;
import de.pocketcloud.network.packet.impl.request.ServerStartRequestPacket;
import de.pocketcloud.network.packet.impl.request.ServerStopRequestPacket;
import de.pocketcloud.network.packet.impl.response.ServerHandshakeResponsePacket;
import de.pocketcloud.network.packet.impl.response.ServerSaveResponsePacket;
import de.pocketcloud.network.packet.impl.response.ServerStartResponsePacket;
import de.pocketcloud.network.packet.impl.response.ServerStopResponsePacket;
import de.pocketcloud.shared.event.server.ServerVerificationDeniedEvent;
import de.pocketcloud.shared.event.server.ServerVerifiedEvent;
import de.pocketcloud.shared.network.packet.type.ActionFailureReason;
import lombok.SneakyThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public final class ServerPacketHandler implements PacketListener {

    @PacketHandler({ServerHandshakeRequestPacket.class})
    public void handle(ServerHandshakeRequestPacket packet, ServerClient sender) {
        var server = (CloudServer) PocketCloud.instance().servers().get(packet.getServerName()).orElse(null);
        if (server == null) return;
        PocketCloud.instance().servers().handleHandshakeReceived(server.name());
        if (PocketCloud.instance().clients().getServer(sender).isEmpty()) {
            if (new ServerRequestVerificationEvent(server).call().isCancelled()) {
                server.logger().warn("Denied server handshake request by event §8(§b{}§8)", sender.address());
                server.verificationStatus(VerificationStatus.DENIED);
                packet.sendResponse(new ServerHandshakeResponsePacket(VerificationStatus.DENIED), sender);
                CloudAPI.instance().events().call(new ServerVerificationDeniedEvent(server));
                return;
            }

            float elapsed = NumberUtils.formatNumber(Duration.between(server.startTime(), Instant.now()).toMillis() / 1000F, 3);

            PocketCloud.instance().clients().add(server, sender);
            CloudLogger.get().success("The server §b{} §rhas §aconnected §rto the cloud. §8(§rTook §b{}§rs§8)", server.name(), elapsed);
            server.data().maxPlayers(packet.getMaxPlayers());
            server.data().processId(packet.getProcessId());
            server.verifiedTime(Instant.now());
            server.verificationStatus(VerificationStatus.VERIFIED);
            server.addToProxies();
            server.sync();
            CloudAPI.instance().events().call(new ServerVerifiedEvent(server));
            packet.sendResponse(new ServerHandshakeResponsePacket(VerificationStatus.VERIFIED), sender);
            server.status(ServerStatus.ONLINE);
        } else {
            CloudLogger.get().warn("Denied server handshake request from §b{} §8(§b{}§8)§r, duplicate server...", packet.getServerName(), sender.address());
            server.verificationStatus(VerificationStatus.DENIED);
            packet.sendResponse(new ServerHandshakeResponsePacket(VerificationStatus.DENIED), sender);
            CloudAPI.instance().events().call(new ServerVerificationDeniedEvent(server));
        }
    }

    @PacketHandler({DisconnectPacket.class})
    public void handle(DisconnectPacket packet, ServerClient sender) {
        CloudServersHandler.handleDisconnect(sender.server(), packet.getReason());
    }

    @PacketHandler({KeepAlivePacket.class})
    public void handle(KeepAlivePacket packet, ServerClient sender) {
        var server = sender.server();
        server.lastKeepAlive(Instant.now());
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

    @SneakyThrows
    @PacketHandler({ServerStartRequestPacket.class})
    public void handle(ServerStartRequestPacket packet, ServerClient sender) {
        var tmpl = PocketCloud.instance().templates().get(packet.getTemplateName()).orElse(null);
        if (tmpl != null) {
            if (PocketCloud.instance().servers().query(ServerSearchQuery.create().ofTemplate(tmpl)).size() < tmpl.settings().maxServerCount()) {
                Collection<String> servers = PocketCloud.instance().servers().start(tmpl, packet.getCount()).get();
                packet.sendResponse(ServerStartResponsePacket.create(ActionFailureReason.NONE, servers), sender);
            } else {
                packet.sendResponse(ServerStartResponsePacket.create(ActionFailureReason.MAX_SERVERS_REACHED, List.of()), sender);
            }
        } else {
            packet.sendResponse(ServerStartResponsePacket.create(ActionFailureReason.TEMPLATE_NOT_FOUND, List.of()), sender);
        }
    }

    @PacketHandler({ServerStopRequestPacket.class})
    public void handle(ServerStopRequestPacket packet, ServerClient sender) {
        PocketCloud.instance().servers().stop(packet.getServer(), packet.isForcefully()).thenSuccess(c -> {
            if (c.isEmpty()) {
                packet.sendResponse(ServerStopResponsePacket.create(ActionFailureReason.SERVER_NOT_FOUND, List.of()), sender);
            } else {
                packet.sendResponse(ServerStopResponsePacket.create(ActionFailureReason.NONE, c), sender);
            }
        });
    }
}