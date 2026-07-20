package de.pocketcloud.shared.component.software;

import de.pocketcloud.api.component.software.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class ServerSoftware implements IServerSoftware {

    protected final String name;
    protected final String templateType;
    protected final ISoftwareDownload download;
    protected final ISoftwareBinary binary;
    protected final ISoftwareBridge bridge;
    protected final ISoftwareConfig config;
}