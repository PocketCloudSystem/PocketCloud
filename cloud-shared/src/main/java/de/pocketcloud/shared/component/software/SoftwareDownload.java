package de.pocketcloud.shared.component.software;

import de.pocketcloud.api.component.software.ISoftwareDownload;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class SoftwareDownload implements ISoftwareDownload {

    protected final String url;
    protected final String filename;
    protected final String[] startCommand;
    protected final boolean checkForUpdates;

    @MapCreator
    public SoftwareDownload(
            @MapKey(name = "url") String url,
            @MapKey(name = "filename") String filename,
            @MapKey(name = "startCommand") String[] startCommand,
            @MapKey(name = "checkForUpdates") boolean checkForUpdates
    ) {
        this.url = url;
        this.filename = filename;
        this.startCommand = startCommand;
        this.checkForUpdates = checkForUpdates;
    }
}