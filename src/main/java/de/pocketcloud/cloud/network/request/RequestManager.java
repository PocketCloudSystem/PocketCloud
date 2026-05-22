package de.pocketcloud.cloud.network.request;

import de.pocketcloud.cloud.network.packet.RequestClientPacket;
import de.pocketcloud.cloud.network.packet.RequestPacketFailureReason;
import de.pocketcloud.cloud.network.packet.ResponseClientPacket;
import de.pocketcloud.cloud.tick.Tickable;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tracks pending {@link RequestClientPacket}s sent from the cloud to sub-servers.
 * Resolves them when the matching {@link ResponseClientPacket} arrives, and rejects
 * them when the 10-second timeout expires.
 */
public final class RequestManager implements Tickable {

    @Getter
    private static RequestManager instance;

    private final Map<String, RequestClientPacket> requests = new HashMap<>();

    public RequestManager() {
        instance = this;
    }

    public RequestClientPacket send(RequestClientPacket packet, io.netty.channel.Channel channel) {
        if (requests.containsKey(packet.getRequestId())) return requests.get(packet.getRequestId());
        packet.prepare();
        channel.writeAndFlush(packet);
        requests.put(packet.getRequestId(), packet);
        return packet;
    }

    public void remove(RequestClientPacket packet) {
        remove(packet.getRequestId());
    }

    public void remove(String requestId) {
        requests.remove(requestId);
    }

    public void resolve(ResponseClientPacket packet) {
        RequestClientPacket request = requests.get(packet.getRequestId());
        if (request != null) {
            request.invokeClosures(false, packet, null);
            remove(request);
        }
    }

    public void reject(RequestClientPacket packet) {
        packet.invokeClosures(true, null, RequestPacketFailureReason.REQUEST_TIMEOUT);
    }

    @Override
    public void tick(long currentTick) {
        long now = System.currentTimeMillis();
        requests.entrySet().removeIf(entry -> {
            RequestClientPacket request = entry.getValue();
            Long sent = request.getSentTimestamp();
            if (sent != null && (now - sent) > 10_000) {
                reject(request);
                return true;
            }

            return false;
        });
    }

    public RequestClientPacket get(String requestId) {
        return requests.get(requestId);
    }

    public Map<String, RequestClientPacket> getAll() {
        return new HashMap<>(requests);
    }
}