package de.pocketcloud.bridge.component;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.bridge.component.storage.CloudServerStorage;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.component.BaseCloudServer;
import de.pocketcloud.shared.component.data.CloudServerData;
import de.pocketcloud.shared.sync.SyncType;

import java.time.Instant;
import java.util.UUID;

public final class CloudServer extends BaseCloudServer implements SyncingElement<ICloudServer> {

    private final CloudServerStorage storage;

    public CloudServer(int id, UUID uuid, String templateName, CloudServerData data, CloudServerStorage storage, ServerStatus status, VerificationStatus verificationStatus, Instant startTime, Instant verifiedTime) {
        super(id, uuid, templateName, data, storage, status, verificationStatus, startTime, verifiedTime);
        this.storage = storage;
    }

    public CloudServer(int id, UUID uuid, String templateName, CloudServerData data, CloudServerStorage storage) {
        super(id, uuid, templateName, data, storage);
        this.storage = storage;
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
}