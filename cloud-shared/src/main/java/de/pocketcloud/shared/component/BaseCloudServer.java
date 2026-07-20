package de.pocketcloud.shared.component;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.shared.component.data.CloudServerData;
import de.pocketcloud.shared.component.storage.BaseCloudServerStorage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Accessors(fluent = true, chain = false)
@AllArgsConstructor
public class BaseCloudServer implements ICloudServer {

    protected final int id;
    protected final UUID uuid;
    protected final String templateName;
    protected final CloudServerData data;
    protected final BaseCloudServerStorage storage;
    protected ServerStatus status = ServerStatus.PENDING;
    protected VerificationStatus verificationStatus = VerificationStatus.PENDING;
    protected Instant startTime = Instant.EPOCH;
    protected Instant verifiedTime = Instant.EPOCH;

    public BaseCloudServer(int id, UUID uuid, String templateName, CloudServerData data, BaseCloudServerStorage storage) {
        this.id = id;
        this.uuid = uuid;
        this.templateName = templateName;
        this.data = data;
        this.storage = storage;
    }
}