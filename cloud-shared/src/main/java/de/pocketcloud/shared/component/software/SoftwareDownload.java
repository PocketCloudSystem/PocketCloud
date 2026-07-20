package de.pocketcloud.shared.component.software;

import de.pocketcloud.api.component.software.ISoftwareDownload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class SoftwareDownload implements ISoftwareDownload {

    protected final String url;
    protected final String filename;
    protected final String startCommand;
    protected final boolean checkForUpdates;
}