package de.pocketcloud.cloud.server.software;

public record SoftwareDownload(
        String url,
        String filename,
        String startCommand,
        boolean checkForUpdates
) {}