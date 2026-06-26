package de.pocketcloud.cloud.server.software;

import java.util.List;

public record SoftwareConfig(
        String mainConfigurationFile,
        String relativeLogFileLocation,
        List<String> savableFiles,
        String saveCommandLine
) {}