package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Node → new head: the newest version the node knows of every unit.
 * {@code hash} is {@code "deleted"} for tombstones; {@code available}: the node can serve the content of that
 * version (its folder matches it, or all files are in its history); {@code head}: the version comes from the time
 * the node was head itself.
 */
@Getter
@NoArgsConstructor
public final class NodeTemplateStatePacket extends ResponsePacket {

    public record Item(String unit, long version, String hash, boolean available, boolean head) {
    }

    private List<Item> items;

    public NodeTemplateStatePacket(List<Item> items) {
        this.items = items;
    }

    @Override
    public void encodePayload(IPacketData data) {
        List<List<Object>> rows = new ArrayList<>();
        for (Item item : items) rows.add(List.of(item.unit(), item.version(), item.hash(), item.available(), item.head()));
        data.writeAll(rows);
    }

    @Override
    public void decodePayload(IPacketData data) {
        items = new ArrayList<>();
        for (Object row : data.readArray()) {
            List<?> r = (List<?>) row;
            items.add(new Item(String.valueOf(r.get(0)), ((Number) r.get(1)).longValue(), String.valueOf(r.get(2)),
                    Boolean.TRUE.equals(r.get(3)), Boolean.TRUE.equals(r.get(4))));
        }
    }
}
