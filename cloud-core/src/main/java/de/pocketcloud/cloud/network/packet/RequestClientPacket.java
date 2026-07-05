package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.common.function.TriConsumer;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.network.request.RequestManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A reversed request: the cloud sends this to a sub-server, and the sub-server answers via ResponseClientPacket.
 * Logic is reversed compared to the regular RequestPacket — here, the cloud is the sender.
 * @see ResponseClientPacket
 */
public abstract class RequestClientPacket extends CloudPacket implements ClientboundPacket {

    @Getter
    private String requestId = null;
    private final List<BiFunction<ResponseClientPacket, Object, Object>> thenClosures = new ArrayList<>();
    private TriConsumer<RequestClientPacket, Throwable, RequestPacketFailureReason> failureHandler = null;

    public void prepare() {
        if (requestId != null) return;
        requestId = UUID.randomUUID().toString();
    }

    @Override
    public final void encode(PacketData packetData) {
        super.encode(packetData);
        packetData.write(requestId);
    }

    @Override
    public final void decode(PacketData packetData) {
        super.decode(packetData);
        requestId = packetData.readString();
    }

    @Override
    public final void decodePayload(PacketData packetData) {}

    @Override
    public final void handle(@NotNull ServerClient client) {}

    public RequestClientPacket sendRequest(ServerClient client) {
        return RequestManager.instance().send(this, client);
    }

    public final void invokeClosures(boolean failed, ResponseClientPacket responsePacket, RequestPacketFailureReason reason, @Nullable Throwable e) {
        if (failed) {
            if (failureHandler != null) {
                failureHandler.accept(this, e, reason);
            }
            return;
        }

        Object value = null;
        try {
            for (BiFunction<ResponseClientPacket, Object, Object> then : thenClosures) {
                value = then.apply(responsePacket, value);
            }
        } catch (Throwable t) {
            if (failureHandler != null) {
                failureHandler.accept(this, t, RequestPacketFailureReason.THEN_CRASHED);
            }
        }
    }

    public RequestClientPacket then(BiFunction<ResponseClientPacket, Object, Object> closure) {
        thenClosures.add(closure);
        return this;
    }

    public RequestClientPacket then(Consumer<ResponseClientPacket> closure) {
        thenClosures.add((packet, prev) -> {
            closure.accept(packet);
            return null;
        });

        return this;
    }

    public RequestClientPacket failure(TriConsumer<RequestClientPacket, Throwable, RequestPacketFailureReason> closure) {
        this.failureHandler = closure;
        return this;
    }

    public boolean isPrepared() {
        return requestId != null;
    }
}