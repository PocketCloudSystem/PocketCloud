package de.pocketcloud.bridge.component;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.bridge.component.storage.CloudServerStorage;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.component.BaseCloudServer;
import de.pocketcloud.shared.component.data.CloudServerData;
import de.pocketcloud.shared.component.storage.BaseCloudServerStorage;
import de.pocketcloud.shared.sync.SyncType;

import java.util.UUID;

public final class CloudServer extends BaseCloudServer implements SyncingElement<ICloudServer> {

    @MapCreator
    public CloudServer(
            @MapKey(name = "id") int id,
            @MapKey(name = "uuid") UUID uuid,
            @MapKey(name = "templateName") String templateName,
            @MapKey(name = "data") CloudServerData data,
            @MapKey(name = "storage", impl = CloudServerStorage.class) BaseCloudServerStorage storage
    ) {
        super(id, uuid, templateName, data, storage);
    }

    @Override
    public void syncIn(ICloudServer server) {
        data().processId(server.data().processId());
        data().tempProcessId(server.data().tempProcessId());
        data().tps(server.data().tps());
        data().avgTps(server.data().avgTps());
        data().memoryUsage(server.data().memoryUsage());
        data().memoryPeak(server.data().memoryPeak());
        data().memoryLimit(server.data().memoryLimit());
        data().cpuUsage(server.data().cpuUsage());
        status = server.status();
        status(server.status());
        verificationStatus(server.verificationStatus());
        storage().syncIn(server.storage().getAll());
    }

    @Override
    public void status(ServerStatus status) {
        super.status(status);
        syncOut();
    }

    @Override
    public void syncOut() {
        SyncPacket.create(SyncType.SERVER_STATUS, pData -> pData.writeAll(name(), status)).sendPacket();
    }

    @Override
    public CloudServerStorage storage() {
        return (CloudServerStorage) super.storage();
    }
}