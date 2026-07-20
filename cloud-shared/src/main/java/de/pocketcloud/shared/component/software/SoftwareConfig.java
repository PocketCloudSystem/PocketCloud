package de.pocketcloud.shared.component.software;

import de.pocketcloud.api.component.software.ISoftwareConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class SoftwareConfig implements ISoftwareConfig {

    protected final String mainConfigurationFile;
    protected final String relativeLogFileLocation;
    protected final List<String> savableFiles;
    protected final String saveCommandLine;
}