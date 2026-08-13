package de.pocketcloud.bridge.platform.wdpe.handler;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.template.TemplateType;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.connection.handler.IJoinHandler;
import dev.waterdog.waterdogpe.network.connection.handler.IReconnectHandler;
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

import java.util.Comparator;

public final class JoinAndFallbackHandler implements IJoinHandler, IReconnectHandler {

    @Override
    public ServerInfo determineServer(ProxiedPlayer proxiedPlayer) {
        return lobbyServer(null);
    }

    @Override
    public ServerInfo getFallbackServer(ProxiedPlayer player, ServerInfo oldServer, ReconnectReason reason, String kickMessage) {
        if (kickMessage.equals("MAINTENANCE")) return null;
        if (reason == ReconnectReason.EXCEPTION || reason == ReconnectReason.UNKNOWN || reason == ReconnectReason.TIMEOUT || reason == ReconnectReason.TRANSFER_FAILED) return lobbyServer(oldServer.getServerName());
        return null;
    }

    private ServerInfo lobbyServer(String excludedServer) {
        return CloudAPI.instance().servers().query(q -> q.ofType(TemplateType.SERVER).lobby(true)).stream()
                .filter(s -> excludedServer == null || !excludedServer.equals(s.name()))
                .min(Comparator.comparingInt(ICloudServer::playerCount))
                .map(s -> ProxyServer.getInstance().getServerInfo(s.name()))
                .orElse(null);
    }
}