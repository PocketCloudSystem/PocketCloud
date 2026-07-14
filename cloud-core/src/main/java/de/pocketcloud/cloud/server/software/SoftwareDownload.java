package de.pocketcloud.cloud.server.software;

import de.pocketcloud.api.model.software.ISoftwareDownload;

public record SoftwareDownload(
        String url,
        String filename,
        String startCommand,
        boolean checkForUpdates
) implements ISoftwareDownload {}