package de.pocketcloud.network.packet;

import de.pocketcloud.api.network.client.IServerClient;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.common.function.TriConsumer;
import de.pocketcloud.common.util.StringUtils;
import de.pocketcloud.network.packet.broadcast.InternalPacketBroadcaster;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Can go both ways: From Cloud to Server & From Server to Cloud
 */
public abstract class RequestPacket extends CloudPacket {

    @Getter
    private String requestId = null;
    private Consumer<ResponsePacket> responseConsumer = null;
    private TriConsumer<RequestPacket, Throwable, RequestPacketFailureReason> failureConsumer = null;

    @Override
    public final void encode(IPacketData packetData) {
        super.encode(packetData);
        if (requestId == null) requestId = StringUtils.generate(12);
        packetData.write(requestId);
    }

    @Override
    public final void decode(IPacketData packetData) {
        super.decode(packetData);
        requestId = packetData.readString();
    }

    /**
     * Only for cloud-bridge
     */
    public RequestPacket sendRequest() {
        sendPacket();
        return this;
    }

    public void sendResponse(ResponsePacket packet) {
        if (!(packet instanceof CloudboundPacket p)) throw new IllegalArgumentException("packet must be a CloudboundPacket");
        packet.setRequestId(requestId);
        InternalPacketBroadcaster.broadcast(new Packet[]{p}, null);
    }

    /**
     * Only for cloud-core
     */
    public RequestPacket sendRequest(IServerClient client) {
        sendPacket(client);
        return this;
    }

    public CompletableFuture<Void> sendResponse(ResponsePacket packet, IServerClient client) {
        if (!(packet instanceof ClientboundPacket p)) throw new IllegalArgumentException("packet must be a ClientboundPacket");
        packet.setRequestId(requestId);
        return client.sendPacket(p);
    }

    public final void invokeClosures(boolean failed, ResponsePacket packet, RequestPacketFailureReason reason, @Nullable Throwable e) {
        if (failed) {
            if (failureConsumer != null) {
                failureConsumer.accept(this, e, reason);
            }
            return;
        }

        try {
            if (responseConsumer != null) responseConsumer.accept(packet);
        } catch (Throwable ex) {
            if (failureConsumer != null) {
                failureConsumer.accept(this, e, RequestPacketFailureReason.THEN_CRASHED);
            }
        }
    }

    public RequestPacket then(Consumer<ResponsePacket> responseConsumer) {
        this.responseConsumer = responseConsumer;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends ResponsePacket> RequestPacket then(Consumer<T> responseConsumer, Class<T> responseClass) {
        this.responseConsumer = (Consumer<ResponsePacket>) responseConsumer;
        return this;
    }

    public RequestPacket failure(TriConsumer<RequestPacket, Throwable, RequestPacketFailureReason> failureConsumer) {
        this.failureConsumer = failureConsumer;
        return this;
    }
}