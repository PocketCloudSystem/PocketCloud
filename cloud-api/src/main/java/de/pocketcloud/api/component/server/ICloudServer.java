package de.pocketcloud.api.component.server;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.api.server.data.ICloudServerData;
import de.pocketcloud.api.server.storage.ICloudServerStorage;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.common.serialization.Writable;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ICloudServer extends Writable<Map<String, Object>> {

    default String name() {
        return templateName() + "-" + id();
    }

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

    void status(ServerStatus status);

    void verificationStatus(VerificationStatus verificationStatus);

    Instant startTime();

    Instant verifiedTime();

    void startTime(Instant startTime);

    void verifiedTime(Instant verifiedTime);

    default Collection<ICloudPlayer> players() {
        return CloudAPI.instance().players().query(q -> q.onServer(this));
    }

    default int playerCount() {
        return players().size();
    }

    default Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }
}