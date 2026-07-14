package de.pocketcloud.cloud.network.request;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.RequestClientPacket;
import de.pocketcloud.cloud.network.packet.RequestPacketFailureReason;
import de.pocketcloud.cloud.network.packet.ResponseClientPacket;
import de.pocketcloud.common.lifecycle.Tickable;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks pending {@link RequestClientPacket}s sent from the cloud to sub-servers.
 * Resolves them when the matching {@link ResponseClientPacket} arrives, and rejects
 * them when the 10-second timeout expires.
 */
public final class RequestManager implements Tickable {

    private final Map<String, RequestClientPacket> requests = new ConcurrentHashMap<>();

    public RequestClientPacket send(RequestClientPacket packet, ServerClient client) {
        if (requests.containsKey(packet.getRequestId())) return requests.get(packet.getRequestId());
        requests.put(packet.getRequestId(), packet);
        client.sendPacket(packet).exceptionally(e -> {
            reject(packet, e);
            remove(packet);
            return null;
        });

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
            request.invokeClosures(false, packet, null, null);
            remove(request);
        }
    }

    public void reject(RequestClientPacket packet) {
        packet.invokeClosures(true, null, RequestPacketFailureReason.REQUEST_TIMEOUT, null);
    }

    public void reject(RequestClientPacket packet, Throwable e) {
        packet.invokeClosures(true, null, RequestPacketFailureReason.EXCEPTION, e);
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