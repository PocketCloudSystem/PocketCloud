package de.pocketcloud.api.model.software;

import java.util.List;

public interface ISoftwareConfig {

    String mainConfigurationFile();

    String relativeLogFileLocation();

    List<String> savableFiles();

    String saveCommandLine();
}