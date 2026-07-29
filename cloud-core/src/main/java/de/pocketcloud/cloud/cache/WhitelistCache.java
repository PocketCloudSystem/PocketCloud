package de.pocketcloud.cloud.cache;

import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.cloud.network.broadcaster.PacketBroadcaster;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.sync.SyncType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class WhitelistCache implements LocalCache<String, Boolean>, SyncingElement<Map<String, Boolean>> {

    private final Map<String, Boolean> whitelist = new ConcurrentHashMap<>();

    @Override
    public void syncIn(Map<String, Boolean> cache) {
        whitelist.clear();
        whitelist.putAll(cache);
    }

    public SyncPacket buildSyncPacket() {
        return SyncPacket.create(SyncType.WHITELIST, data -> data.write(whitelist));
    }

    @Override
    public void syncOut() {
        PacketBroadcaster.broadcast(buildSyncPacket());
    }

    @Override
    public void add(String key, @NotNull Boolean value) {
        whitelist.put(key, value);
        syncOut();
    }

    @Override
    public void remove(String element) {
        if (whitelist.remove(element) != null)
            syncOut();
    }

    @Override
    public void clear() {
        whitelist.clear();
        syncOut();
    }

    @Override
    public boolean contains(String element) {
        return whitelist.containsKey(element);
    }

    @Override
    public int size() {
        return whitelist.size();
    }

    @Override
    public Optional<Boolean> get(String id) {
        return Optional.ofNullable(whitelist.get(id));
    }

    @Override
    public Map<String, Boolean> getAll() {
        return Collections.unmodifiableMap(whitelist);
    }
}