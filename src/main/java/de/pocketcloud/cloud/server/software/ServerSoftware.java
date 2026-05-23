package de.pocketcloud.cloud.server.software;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.util.DownloadProgressBar;
import de.pocketcloud.cloud.template.TemplateType;
import de.pocketcloud.cloud.util.FileUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.cloud.util.net.NetUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public record ServerSoftware(
        String name,
        String templateType,
        SoftwareDownload download,
        SoftwareBinary binary,
        SoftwareBridge bridge,
        SoftwareConfig config
) {

    public boolean downloadSoftware() {
        DownloadProgressBar progressBar = new DownloadProgressBar(download.filename, PocketCloud.getInstance().console().getTerminal(), PocketCloud.getInstance().console().getReader());
        progressBar.start();

        try {
            NetUtils.download(download.url, directoryPath().resolve(download.filename), progressBar::update);

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
        if (!download.checkForUpdates) return !sourceFile().exists();
        File sizeFile = sizeFilePath().toFile();
        if (sizeFile.exists()) {
            try {
                String content = Files.readString(sizeFile.toPath());
                long size = Long.parseLong(content);
                return size != NetUtils.downloadSize(download.url);
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to check for updates for {}", e, name);
                return true;
            }
        }

        return true;
    }

    public String replacePlaceholder(String subject) {
        return subject.replace("{BINARY_PATH}", binary.directoryPath(this).toAbsolutePath().toString())
                .replace("{SOFTWARE_PATH}", directoryPath().toAbsolutePath().toString());
    }

    public String normalizedName() {
        return name.toLowerCase().replaceAll(" ", "_");
    }

    public File sourceFile() {
        return directoryPath().resolve(download.filename).toFile();
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

    public record SoftwareDownload(
            String url,
            String filename,
            String startCommand,
            boolean checkForUpdates
    ) {}

    /**
     * Basically, the URL has to be a download link for either a .zip or .gz file.
     * This file should contain all the relevant binary files inside one single root folder (name can be anything)
     * Example:
     * php-8.4.tar.gz
     *  -> bin
     *      -> php7
     *          -> bin
     *              -> [php.exe, php.ini, etc...]
     * <p>
     * This `bin` folder will then be extracted inside the software/{software}/binary/ folder.
     * If the url is either null or blank, `java` will be the result of the placeholder {BINARY_PATH}
     * inside the `download` field in the respective config.
     * @param url
     */
    public record SoftwareBinary(
            String url,
            boolean checkForUpdates
    ) {

        public boolean download(ServerSoftware parent) {
            if (url == null || url.isBlank()) return true;
            Path binaryDir = directoryPath(parent);
            String filename;
            try {
                filename = Path.of(new URI(url).getPath()).getFileName().toString();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            Path archivePath = binaryDir.resolve(filename);

            DownloadProgressBar progressBar = new DownloadProgressBar(filename, PocketCloud.getInstance().console().getTerminal(), PocketCloud.getInstance().console().getReader());
            progressBar.start();

            try {
                Files.createDirectories(binaryDir);
            } catch (IOException e) {
                progressBar.abort();
                return false;
            }

            try {
                NetUtils.download(url, archivePath, progressBar::update);

                if (filename.endsWith(".zip")) {
                    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archivePath))) {
                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            extractEntry(entry.getName(), entry.isDirectory(), zis, binaryDir);
                            zis.closeEntry();
                        }
                    }
                } else if (filename.endsWith(".tar.gz") || filename.endsWith(".gz")) {
                    try (TarArchiveInputStream tis = new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(archivePath)))) {
                        TarArchiveEntry entry;
                        while ((entry = tis.getNextEntry()) != null) {
                            extractEntry(entry.getName(), entry.isDirectory(), tis, binaryDir);
                        }
                    }
                }

                File sizeFile = sizeFile(parent).toFile();
                if (!sizeFile.exists() && !sizeFile.createNewFile()) throw new IOException("Failed to create .size file for binary");
                FileUtils.filePutContents(sizeFile(parent), String.valueOf(archivePath.toFile().length()));

                Files.deleteIfExists(archivePath);
                progressBar.finish();
                return true;
            } catch (Exception e) {
                progressBar.abort();
                return false;
            }
        }

        public boolean requiresUpdate(ServerSoftware parent) {
            try (Stream<Path> stream = Files.list(directoryPath(parent))) {
                if (stream.count() <= 1) return true;
                if (!checkForUpdates) return !directoryPath(parent).toFile().exists();

                File sizeFile = sizeFile(parent).toFile();
                if (sizeFile.exists()) {
                    try {
                        String content = Files.readString(sizeFile.toPath());
                        long size = Long.parseLong(content);
                        return size != NetUtils.downloadSize(url);
                    } catch (Exception e) {
                        CloudLogger.get().exception("Failed to check for updates for {} for binary", e, parent.name());
                        return true;
                    }
                }
            } catch (IOException _) {}

            return true;
        }

        private void extractEntry(String entryName, boolean isDirectory, InputStream is, Path binaryDir) throws IOException {
            if (entryName.isEmpty()) return;

            Path target = binaryDir.resolve(entryName).normalize();
            if (!target.startsWith(binaryDir)) throw new IOException("Zip slip detected: " + entryName);

            if (isDirectory) {
                Files.createDirectories(target);
            } else {
                Files.createDirectories(target.getParent());
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        public Path directoryPath(ServerSoftware parent) {
            return parent.directoryPath().resolve("binary");
        }

        public Path sizeFile(ServerSoftware parent) {
            return directoryPath(parent).resolve(".size");
        }
    }

    public record SoftwareBridge(
            String url,
            String relativeServerPath,
            boolean checkForUpdates
    ) {

        public boolean download(ServerSoftware parent) {
            DownloadProgressBar progressBar = new DownloadProgressBar(fileName(), PocketCloud.getInstance().console().getTerminal(), PocketCloud.getInstance().console().getReader());
            progressBar.start();

            try {
                NetUtils.download(url, sourceFile(parent), progressBar::update);

                File sizeFile = sizeFile(parent).toFile();
                if (!sizeFile.exists() && !sizeFile.createNewFile()) throw new IOException("Failed to create .size file for bridge");
                FileUtils.filePutContents(sizeFile(parent), String.valueOf(sourceFile(parent).toFile().length()));

                progressBar.finish();
                return true;
            } catch (Exception e) {
                progressBar.abort();
                return false;
            }
        }

        public Path directoryPath(ServerSoftware parent) {
            return parent.directoryPath().resolve("bridge");
        }

        public String fileName() {
            return Path.of(relativeServerPath).getFileName().toString();
        }

        public Path sourceFile(ServerSoftware parent) {
            return directoryPath(parent).resolve(fileName());
        }

        public Path sizeFile(ServerSoftware parent) {
            return directoryPath(parent).resolve(".size");
        }

        public boolean requiresUpdate(ServerSoftware parent) {
            if (!checkForUpdates) return !sourceFile(parent).toFile().exists();
            File sizeFile = sizeFile(parent).toFile();
            if (sizeFile.exists()) {
                try {
                    String content = Files.readString(sizeFile.toPath());
                    long size = Long.parseLong(content);
                    return size != NetUtils.downloadSize(url);
                } catch (Exception e) {
                    CloudLogger.get().exception("Failed to check for updates for {} for bridge", e, parent.name);
                    return true;
                }
            }

            return true;
        }
    }

    public record SoftwareConfig(
            String mainConfigurationFile,
            String relativeLogFileLocation,
            List<String> savableFiles,
            String saveCommandLine
    ) {}
}