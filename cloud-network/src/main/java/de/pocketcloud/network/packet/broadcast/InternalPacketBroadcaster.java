package de.pocketcloud.network.packet.broadcast;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.network.packet.PacketExcluder;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiConsumer;

@ApiStatus.Internal
public final class InternalPacketBroadcaster {

    @Setter
    private static BiConsumer<Packet[], PacketExcluder> broadcasterHandler = null;

    public static void broadcast(Packet[] packets, PacketExcluder excluder) {
        if (broadcasterHandler != null) {
            broadcasterHandler.accept(packets, excluder);
        }
    }
}