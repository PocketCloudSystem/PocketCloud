package de.pocketcloud.cloud.server.software;

import de.pocketcloud.api.model.software.ISoftwareBridge;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.util.DownloadProgressBar;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.common.util.NetUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SoftwareBridge implements ISoftwareBridge {

    private transient ServerSoftware parent;

    private final String url;
    private final String relativeServerPath;
    private final boolean checkForUpdates;

    public SoftwareBridge(String url, String relativeServerPath, boolean checkForUpdates) {
        this.url = url;
        this.relativeServerPath = relativeServerPath;
        this.checkForUpdates = checkForUpdates;
    }

    void setParent(ServerSoftware parent) {
        this.parent = parent;
    }

    public boolean download() {
        DownloadProgressBar progressBar = new DownloadProgressBar(fileName(), PocketCloud.instance().console().getTerminal(), PocketCloud.instance().console().getReader());
        progressBar.start();

        try {
            NetUtils.download(url, sourceFile(), progressBar::update);

            File sizeFile = sizeFile().toFile();
            if (!sizeFile.exists() && !sizeFile.createNewFile()) throw new IOException("Failed to create .size file for bridge");
            FileUtils.filePutContents(sizeFile(), String.valueOf(sourceFile().toFile().length()));

            progressBar.finish();
            return true;
        } catch (Exception e) {
            progressBar.abort();
            return false;
        }
    }

    public boolean requiresUpdate() {
        if (!checkForUpdates) return !sourceFile().toFile().exists();
        File sizeFile = sizeFile().toFile();
        if (sizeFile.exists()) {
            try {
                String content = Files.readString(sizeFile.toPath());
                long size = Long.parseLong(content);
                return size != NetUtils.downloadSize(url);
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to check for updates for {} for bridge", e, parent.name());
                return true;
            }
        }

        return true;
    }

    public String url() {
        return url;
    }

    public String relativeServerPath() {
        return relativeServerPath;
    }

    public boolean checkForUpdates() {
        return checkForUpdates;
    }

    public Path directoryPath() {
        return parent.directoryPath().resolve("bridge");
    }

    public String fileName() {
        return Path.of(relativeServerPath).getFileName().toString();
    }

    public Path sourceFile() {
        return directoryPath().resolve(fileName());
    }

    public Path sizeFile() {
        return directoryPath().resolve(".size");
    }
}