package de.pocketcloud.shared.component;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.common.serialization.annotation.MapKey;
import de.pocketcloud.shared.component.data.CloudServerData;
import de.pocketcloud.shared.component.storage.BaseCloudServerStorage;
import de.pocketcloud.shared.converter.InstantConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Accessors(fluent = true, chain = false)
public class BaseCloudServer implements ICloudServer {

    protected final int id;
    protected final UUID uuid;
    protected final String templateName;
    protected final CloudServerData data;
    protected final BaseCloudServerStorage storage;
    @Setter
    protected ServerStatus status = ServerStatus.PENDING;
    @Setter
    protected VerificationStatus verificationStatus = VerificationStatus.PENDING;
    @MapKey(converter = InstantConverter.class)
    protected Instant startTime = Instant.EPOCH;
    @MapKey(converter = InstantConverter.class)
    protected Instant verifiedTime = Instant.EPOCH;

    public BaseCloudServer(int id, UUID uuid, String templateName, CloudServerData data, BaseCloudServerStorage storage) {
        this.id = id;
        this.uuid = uuid;
        this.templateName = templateName;
        this.data = data;
        this.storage = storage;
    }
}