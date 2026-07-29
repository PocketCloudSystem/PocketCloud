package de.pocketcloud.shared.component.software;

import de.pocketcloud.api.component.software.*;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class ServerSoftware implements IServerSoftware {

    protected final String name;
    protected final String templateType;
    protected final ISoftwareDownload download;
    protected final ISoftwareBinary binary;
    protected final ISoftwareBridge bridge;
    protected final ISoftwareConfig config;

    @MapCreator
    public ServerSoftware(
            @MapKey(name = "name") String name,
            @MapKey(name = "templateType") String templateType,
            @MapKey(name = "download", impl = SoftwareDownload.class) ISoftwareDownload download,
            @MapKey(name = "binary", impl = SoftwareBinary.class) ISoftwareBinary binary,
            @MapKey(name = "bridge", impl = SoftwareBridge.class) ISoftwareBridge bridge,
            @MapKey(name = "config", impl = SoftwareConfig.class) ISoftwareConfig config
    ) {
        this.name = name;
        this.templateType = templateType;
        this.download = download;
        this.binary = binary;
        this.bridge = bridge;
        this.config = config;
    }
}