package de.pocketcloud.api.model.server;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.api.server.data.ICloudServerData;
import de.pocketcloud.api.server.storage.ICloudServerStorage;

import java.time.Instant;
import java.util.UUID;

public interface ICloudServer {

    String name();

    int id();

    UUID uuid();

    String templateName();

    default ITemplate template() {
        return CloudAPI.instance().templates().get(templateName()).orElseThrow(() -> new IllegalStateException("Template should not be null"));
    }

    ICloudServerData data();

    ICloudServerStorage storage();

    ServerStatus status();

    VerificationStatus verificationStatus();

    Instant startTime();

    Instant verifiedTime();
}