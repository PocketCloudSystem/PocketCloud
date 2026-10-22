package de.pocketcloud.bridge.component.storage;

import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.component.storage.BaseCloudServerStorage;
import de.pocketcloud.shared.sync.SyncType;

import java.util.Map;
import java.util.UUID;

public final class CloudServerStorage extends BaseCloudServerStorage {

    @MapCreator
    public CloudServerStorage(@MapKey(name = "serverUuid") UUID serverUuid) {
        super(serverUuid);
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