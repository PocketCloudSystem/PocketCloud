package de.pocketcloud.cloud.network.request;

import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.network.packet.RequestPacket;
import de.pocketcloud.network.packet.RequestPacketFailureReason;
import de.pocketcloud.network.packet.ResponsePacket;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class RequestManager implements Tickable {

    private final Map<String, RequestPacket> requests = new ConcurrentHashMap<>();

    public RequestPacket add(RequestPacket packet) {
        requests.put(packet.getRequestId(), packet);
        return packet;
    }

    public void remove(RequestPacket packet) {
        remove(packet.getRequestId());
    }

    public void remove(String requestId) {
        requests.remove(requestId);
    }

    public void resolve(ResponsePacket packet) {
        RequestPacket request = requests.get(packet.getRequestId());
        if (request != null) {
            request.invokeClosures(false, packet, null, null);
            remove(request);
        }
    }

    public void reject(RequestPacket packet) {
        packet.invokeClosures(true, null, RequestPacketFailureReason.REQUEST_TIMEOUT, null);
        remove(packet);
    }

    public void reject(RequestPacket packet, Throwable e) {
        packet.invokeClosures(true, null, RequestPacketFailureReason.EXCEPTION, e);
        remove(packet);
    }

    @Override
    public void tick(long currentTick) {
        long now = System.currentTimeMillis();
        requests.entrySet().removeIf(entry -> {
            RequestPacket request = entry.getValue();
            Long sent = request.getSentTimestamp();
            if (sent != null && (now - sent) > 10_000) {
                reject(request);
                return true;
            }

            return false;
        });
    }

    public Optional<RequestPacket> get(String requestId) {
        return Optional.ofNullable(requests.getOrDefault(requestId, null));
    }

    public Map<String, RequestPacket> getAll() {
        return new HashMap<>(requests);
    }
}