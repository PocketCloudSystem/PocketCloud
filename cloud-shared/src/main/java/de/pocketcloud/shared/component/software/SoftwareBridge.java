package de.pocketcloud.shared.component.software;

import de.pocketcloud.api.component.software.ISoftwareBridge;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.file.Path;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class SoftwareBridge implements ISoftwareBridge {

    public final String url;
    protected final String relativeServerPath;
    protected final boolean checkForUpdates;

    public String fileName() {
        return Path.of(relativeServerPath).getFileName().toString();
    }
}