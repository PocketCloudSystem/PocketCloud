package de.pocketcloud.cloud.server.software;

import de.pocketcloud.api.model.software.ISoftwareBinary;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.util.DownloadProgressBar;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.common.util.NetUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

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
 */
public final class SoftwareBinary implements ISoftwareBinary {

    private transient ServerSoftware parent;

    private final String url;
    private final boolean checkForUpdates;

    public SoftwareBinary(String url, boolean checkForUpdates) {
        this.url = url;
        this.checkForUpdates = checkForUpdates;
    }

    void setParent(ServerSoftware parent) {
        this.parent = parent;
    }

    public boolean download() {
        if (url == null || url.isBlank()) return true;
        Path binaryDir = directoryPath();
        String filename;
        try {
            filename = Path.of(new URI(url).getPath()).getFileName().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Path archivePath = binaryDir.resolve(filename);

        DownloadProgressBar progressBar = new DownloadProgressBar(filename, PocketCloud.instance().console().getTerminal(), PocketCloud.instance().console().getReader());
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
                try (ZipArchiveInputStream zis = new ZipArchiveInputStream(Files.newInputStream(archivePath))) {
                    ZipArchiveEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        extractZipEntry(entry, zis, binaryDir);
                    }
                }
            } else if (filename.endsWith(".tar.gz") || filename.endsWith(".gz")) {
                try (TarArchiveInputStream tis = new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(archivePath)))) {
                    TarArchiveEntry entry;
                    while ((entry = tis.getNextEntry()) != null) {
                        extractTarEntry(entry, tis, binaryDir);
                    }
                }
            }

            File sizeFile = sizeFile().toFile();
            if (!sizeFile.exists() && !sizeFile.createNewFile()) throw new IOException("Failed to create .size file for binary");
            FileUtils.filePutContents(sizeFile(), String.valueOf(archivePath.toFile().length()));

            Files.deleteIfExists(archivePath);

            try (var stream = Files.walk(binaryDir)) {
                stream.filter(Files::isRegularFile)
                        .forEach(path -> path.toFile().setExecutable(true));
            }

            progressBar.finish();
            return true;
        } catch (Exception e) {
            progressBar.abort();
            return false;
        }
    }

    public boolean requiresUpdate() {
        try (Stream<Path> stream = Files.list(directoryPath())) {
            if (stream.count() <= 1) return true;
            if (!checkForUpdates) return !directoryPath().toFile().exists();

            File sizeFile = sizeFile().toFile();
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

    private void extractZipEntry(ZipArchiveEntry entry, ZipArchiveInputStream zis, Path binaryDir) throws IOException {
        if (entry.getName().isEmpty()) return;

        Path target = binaryDir.resolve(entry.getName()).normalize();
        if (!target.startsWith(binaryDir)) throw new IOException("Zip slip detected: " + entry.getName());

        if (entry.isDirectory()) {
            Files.createDirectories(target);
        } else {
            Files.createDirectories(target.getParent());

            int unixMode = entry.getUnixMode();
            boolean isSymlink = (unixMode & 0xA000) == 0xA000;

            if (isSymlink) {
                String linkTarget = new String(zis.readAllBytes()).trim();
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) Files.delete(target);
                Files.createSymbolicLink(target, Path.of(linkTarget));
            } else {
                try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private void extractTarEntry(TarArchiveEntry entry, TarArchiveInputStream tis, Path binaryDir) throws IOException {
        if (entry.getName().isEmpty()) return;

        Path target = binaryDir.resolve(entry.getName()).normalize();
        if (!target.startsWith(binaryDir)) throw new IOException("Zip slip detected: " + entry.getName());

        if (entry.isDirectory()) {
            Files.createDirectories(target);
        } else if (entry.isSymbolicLink()) {
            Files.createDirectories(target.getParent());
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) Files.delete(target);
            Files.createSymbolicLink(target, Path.of(entry.getLinkName()));
        } else if (entry.isLink()) {
            Files.createDirectories(target.getParent());
            Path linkTarget = binaryDir.resolve(entry.getLinkName()).normalize();
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) Files.delete(target);
            Files.createLink(target, linkTarget);
        } else {
            Files.createDirectories(target.getParent());
            try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = tis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        }
    }

    public String url() {
        return url;
    }

    public boolean checkForUpdates() {
        return checkForUpdates;
    }

    public Path directoryPath() {
        return parent.directoryPath().resolve("binary");
    }

    public Path sizeFile() {
        return directoryPath().resolve(".size");
    }
}