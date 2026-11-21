package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Head → node: all units with version and hash ({@code "deleted"} for tombstones).
 * {@code ready = false} while the head is still building its state.
 */
@Getter
@NoArgsConstructor
public final class HeadTemplateListPacket extends ResponsePacket {

    public record Item(String unit, long version, String hash) {
    }

    private boolean ready;
    private List<Item> items;

    public HeadTemplateListPacket(boolean ready, List<Item> items) {
        this.ready = ready;
        this.items = items;
    }

    @Override
    public void encodePayload(IPacketData data) {
        List<List<Object>> rows = new ArrayList<>();
        for (Item item : items) rows.add(List.of(item.unit(), item.version(), item.hash()));
        data.writeAll(ready, rows);
    }

    @Override
    public void decodePayload(IPacketData data) {
        ready = data.readBool();
        items = new ArrayList<>();
        for (Object row : data.readArray()) {
            List<?> r = (List<?>) row;
            items.add(new Item(String.valueOf(r.get(0)), ((Number) r.get(1)).longValue(), String.valueOf(r.get(2))));
        }
    }
}
