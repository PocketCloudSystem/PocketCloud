package de.pocketcloud.bridge.component.storage;

import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.component.storage.BaseCloudServerStorage;
import de.pocketcloud.shared.sync.SyncType;

import java.util.Map;
import java.util.UUID;

public final class CloudServerStorage extends BaseCloudServerStorage {

    public CloudServerStorage(UUID serverUuid) {
        super(serverUuid);
    }

    public CloudServerStorage(UUID serverUuid, Map<String, Object> storage) {
        super(serverUuid, storage);
    }

    @Override
    public void syncIn(Map<String, Object> data) {
        storage.clear();
        storage.putAll(data);
    }

    @Override
    public void syncOut() {
        SyncPacket.create(SyncType.SERVER_STORAGE, data -> data.writeAll(serverUuid.toString(), storage)).broadcast();
    }
}