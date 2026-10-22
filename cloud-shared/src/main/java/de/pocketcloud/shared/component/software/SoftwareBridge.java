package de.pocketcloud.shared.component.software;

import de.pocketcloud.api.component.software.ISoftwareBridge;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.file.Path;

@Getter
@Accessors(fluent = true)
public class SoftwareBridge implements ISoftwareBridge {

    public final String url;
    protected final String relativeServerPath;
    protected final boolean checkForUpdates;

    @MapCreator
    public SoftwareBridge(
            @MapKey(name = "url") String url,
            @MapKey(name = "relativeServerPath") String relativeServerPath,
            @MapKey(name = "checkForUpdates") boolean checkForUpdates
    ) {
        this.url = url;
        this.relativeServerPath = relativeServerPath;
        this.checkForUpdates = checkForUpdates;
    }

    public String fileName() {
        return Path.of(relativeServerPath).getFileName().toString();
    }
}