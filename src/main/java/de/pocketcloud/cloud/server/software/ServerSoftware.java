package de.pocketcloud.cloud.server.software;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.util.DownloadProgressBar;
import de.pocketcloud.cloud.template.TemplateType;
import de.pocketcloud.cloud.util.FileUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.cloud.util.net.NetUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public record ServerSoftware(
        String name,
        String templateType,
        SoftwareDownload download,
        SoftwareBinary binary,
        SoftwareBridge bridge,
        SoftwareConfig config
) {

    public ServerSoftware {
        binary.setParent(this);
        bridge.setParent(this);
    }

    public boolean downloadSoftware() {
        DownloadProgressBar progressBar = new DownloadProgressBar(download.filename(), PocketCloud.instance().console().getTerminal(), PocketCloud.instance().console().getReader());
        progressBar.start();

        try {
            NetUtils.download(download.url(), directoryPath().resolve(download.filename()), progressBar::update);

            File sizeFile = sizeFilePath().toFile();
            if (!sizeFile.exists() && !sizeFile.createNewFile()) throw new IOException("Failed to create .size file");
            FileUtils.filePutContents(sizeFilePath(), String.valueOf(sourceFile().length()));

            progressBar.finish();
            return true;
        } catch (Exception e) {
            progressBar.abort();
            return false;
        }
    }

    public boolean requiresUpdate() {
        if (!download.checkForUpdates()) return !sourceFile().exists();
        File sizeFile = sizeFilePath().toFile();
        if (sizeFile.exists()) {
            try {
                String content = Files.readString(sizeFile.toPath());
                long size = Long.parseLong(content);
                return size != NetUtils.downloadSize(download.url());
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to check for updates for {}", e, name);
                return true;
            }
        }

        return true;
    }

    public String replacePlaceholder(String subject) {
        return subject.replace("{BINARY_PATH}", binary.directoryPath().toAbsolutePath().toString())
                .replace("{SOFTWARE_PATH}", directoryPath().toAbsolutePath().toString());
    }

    public String normalizedName() {
        return name.toLowerCase().replace(" ", "_");
    }

    public File sourceFile() {
        return directoryPath().resolve(download.filename()).toFile();
    }

    public Path sizeFilePath() {
        return directoryPath().resolve(".size");
    }

    public Path directoryPath() {
        return Path.of("storage/software/" + normalizedName());
    }

    public Path configFilePath() {
        return PocketCloudPaths.storage().software().with(normalizedName() + ".json").asPath();
    }

    public TemplateType type() {
        try {
            return TemplateType.valueOf(templateType);
        } catch (IllegalArgumentException _) {
            return null;
        }
    }
}