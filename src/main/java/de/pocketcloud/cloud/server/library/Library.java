package de.pocketcloud.cloud.server.library;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.util.DownloadProgressBar;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.util.FileUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.cloud.util.net.NetUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public record Library(
        String name,
        String downloadUrl,
        String namespacePrefix,
        String namespaceFolder,
        List<String> softwareList,
        boolean checkForUpdates
) {

    /**
     * When downloaded a library, the cloud expects the zip archive to have the following structure:
     * -> library.zip -> Library-main (or any name) -> the actual library contents
     * <p>
     * The extracted contents will be placed inside storage/libraries/{name}/
     * If softwares is empty, the library is available to all softwares.
     */
    public boolean download() {
        Path libPath = directoryPath();
        FileUtils.removeDirectory(libPath.toAbsolutePath());

        DownloadProgressBar progressBar = new DownloadProgressBar(name, PocketCloud.instance().console().getTerminal(), PocketCloud.instance().console().getReader());
        progressBar.start();

        Path archivePath = libPath.getParent().resolve(name + ".zip");
        try {
            Files.createDirectories(libPath);
            NetUtils.download(downloadUrl, archivePath, progressBar::update);

            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archivePath))) {
                ZipEntry entry;
                String rootFolder = null;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();

                    if (rootFolder == null) {
                        int slash = entryName.indexOf("/");
                        rootFolder = slash == -1 ? entryName : entryName.substring(0, slash);
                    }

                    int slashIndex = entryName.indexOf("/");
                    if (slashIndex == -1) {
                        zis.closeEntry();
                        continue;
                    }

                    String stripped = entryName.substring(slashIndex + 1);
                    if (stripped.isEmpty()) {
                        zis.closeEntry();
                        continue;
                    }

                    Path target = libPath.resolve(stripped).normalize();
                    if (!target.startsWith(libPath)) throw new IOException("Zip slip detected: " + entryName);

                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                    }

                    zis.closeEntry();
                }
            }

            File sizeFile = sizeFilePath().toFile();
            if (!sizeFile.exists() && !sizeFile.createNewFile()) throw new IOException("Failed to create .size file for library: " + name);
            FileUtils.filePutContents(sizeFilePath(), String.valueOf(archivePath.toFile().length()));

            progressBar.finish();
            return true;
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to download library: {}", e, name);
            progressBar.abort();
            return false;
        } finally {
            try {
                Files.deleteIfExists(archivePath);
            } catch (IOException _) {}
        }
    }

    public boolean requiresUpdate() {
        if (!checkForUpdates) return !Files.isDirectory(directoryPath());
        boolean sizeMismatch = false;
        File libDir = directoryPath().toFile();
        File sizeFile = sizeFilePath().toFile();
        if (sizeFile.exists()) {
            try {
                long size = Long.parseLong(Files.readString(sizeFile.toPath()).trim());
                long latestSize = NetUtils.downloadSize(downloadUrl);
                sizeMismatch = latestSize != -1 && size != latestSize;
                if (latestSize == -1) {
                    CloudLogger.get().warn("Result of download size for §b{} §rreturns §c-1§r.", downloadUrl)
                            .warn("Please update the library manually if needed.");
                    Thread.sleep(1500);
                }
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to check for updates for library {}", e, name);
            }
        } else sizeMismatch = true;

        if (sizeMismatch) return true;
        return !libDir.exists() ||
                !libDir.isDirectory() ||
                Objects.requireNonNull(libDir.list()).length < 2 ||
                !Files.isDirectory(directoryPath().resolve(namespaceFolder));
    }

    public boolean isAvailableFor(ServerSoftware software) {
        return softwareList.isEmpty() || softwareList.contains(software.name());
    }

    public Path directoryPath() {
        return PocketCloudPaths.storage().libraries().with(name).asPath();
    }

    public Path sizeFilePath() {
        return directoryPath().resolve(".size");
    }
}