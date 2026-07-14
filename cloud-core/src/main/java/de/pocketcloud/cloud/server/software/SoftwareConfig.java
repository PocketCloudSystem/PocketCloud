package de.pocketcloud.cloud.server.software;

import de.pocketcloud.api.model.software.ISoftwareConfig;

import java.util.List;

public record SoftwareConfig(
        String mainConfigurationFile,
        String relativeLogFileLocation,
        List<String> savableFiles,
        String saveCommandLine
) implements ISoftwareConfig {}