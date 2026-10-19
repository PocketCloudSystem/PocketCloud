package de.pocketcloud.network.request;

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
        if (packet != null) {
            requests.remove(packet.getRequestId(), packet);
        }
    }

    public void remove(String requestId) {
        requests.remove(requestId);
    }

    public void resolve(ResponsePacket packet) {
        RequestPacket request = requests.remove(packet.getRequestId());
        if (request != null) {
            request.invokeClosures(false, packet, null, null);
        }
    }

    public void reject(RequestPacket packet) {
        if (requests.remove(packet.getRequestId(), packet)) {
            invokeTimeoutClosures(packet);
        }
    }

    public void reject(RequestPacket packet, Throwable e) {
        if (requests.remove(packet.getRequestId(), packet)) {
            invokeExceptionClosures(packet, e);
        }
    }

    private void invokeTimeoutClosures(RequestPacket packet) {
        packet.invokeClosures(true, null, RequestPacketFailureReason.REQUEST_TIMEOUT, null);
    }

    private void invokeExceptionClosures(RequestPacket packet, Throwable e) {
        packet.invokeClosures(true, null, RequestPacketFailureReason.EXCEPTION, e);
    }

    @Override
    public void tick(long currentTick) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, RequestPacket> entry : requests.entrySet()) {
            RequestPacket request = entry.getValue();
            Long sent = request.getSentTimestamp();
            if (sent != null && (now - sent) > request.getTimeoutMs()) {
                if (requests.remove(entry.getKey(), request)) {
                    invokeTimeoutClosures(request);
                }
            }
        }
    }

    public Optional<RequestPacket> get(String requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }

    public Map<String, RequestPacket> getAll() {
        return new HashMap<>(requests);
    }
}