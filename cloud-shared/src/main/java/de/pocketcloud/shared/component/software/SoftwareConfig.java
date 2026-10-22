package de.pocketcloud.shared.component.software;

import de.pocketcloud.api.component.software.ISoftwareConfig;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Accessors(fluent = true)
public class SoftwareConfig implements ISoftwareConfig {

    protected final String mainConfigurationFile;
    protected final String relativeLogFileLocation;
    protected final List<String> savableFiles;
    protected final String saveCommandLine;

    @MapCreator
    public SoftwareConfig(
            @MapKey(name = "mainConfigurationFile") String mainConfigurationFile,
            @MapKey(name = "relativeLogFileLocation") String relativeLogFileLocation,
            @MapKey(name = "savableFiles") List<String> savableFiles,
            @MapKey(name = "saveCommandLine") String saveCommandLine
    ) {
        this.mainConfigurationFile = mainConfigurationFile;
        this.relativeLogFileLocation = relativeLogFileLocation;
        this.savableFiles = savableFiles;
        this.saveCommandLine = saveCommandLine;
    }
}