package de.pocketcloud.cloud.server.software;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.util.DownloadProgressBar;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.common.util.NetUtils;
import de.pocketcloud.shared.component.software.ServerSoftware;
import de.pocketcloud.shared.component.software.SoftwareBinary;
import de.pocketcloud.shared.component.software.SoftwareBridge;
import de.pocketcloud.shared.component.software.SoftwareDownload;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public final class SoftwareService {

    private final Map<String, Long> cachedOnlineSizes = new ConcurrentHashMap<>();

    public boolean downloadSoftware(ServerSoftware software) {
        SoftwareDownload download = (SoftwareDownload) software.download();
        DownloadProgressBar progressBar = createProgressBar(download.filename());
        progressBar.start();

        try {
            NetUtils.download(download.url(), directoryPath(software).resolve(download.filename()), progressBar::update);

            File sizeFile = sizeFilePath(software).toFile();
            if (!sizeFile.exists() && !sizeFile.createNewFile()) throw new IOException("Failed to create .size file");
            FileUtils.filePutContents(sizeFilePath(software), String.valueOf(sourceFile(software).length()));

            progressBar.finish();
            return true;
        } catch (Exception e) {
            progressBar.abort();
            return false;
        }
    }

    public boolean requiresUpdateSoftware(ServerSoftware software) {
        SoftwareDownload download = (SoftwareDownload) software.download();
        if (!download.checkForUpdates()) return !sourceFile(software).exists();

        File sizeFile = sizeFilePath(software).toFile();
        long onlineSize = cachedOnlineSizes.computeIfAbsent(download.url(), (_) -> NetUtils.downloadSize(download.url()));
        if (sizeFile.exists()) {
            try {
                String content = Files.readString(sizeFile.toPath());
                long size = Long.parseLong(content);
                return size != onlineSize;
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to check for updates for {}", e, software.name());
                return true;
            }
        }

        return true;
    }

    public boolean downloadBinary(ServerSoftware software) {
        SoftwareBinary binary = (SoftwareBinary) software.binary();
        if (binary.url() == null || binary.url().isBlank()) return true;

        Path binaryDir = binaryDirectoryPath(software);
        String filename;
        try {
            filename = Path.of(new URI(binary.url()).getPath()).getFileName().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Path archivePath = binaryDir.resolve(filename);
        DownloadProgressBar progressBar = createProgressBar(filename);
        progressBar.start();

        try {
            Files.createDirectories(binaryDir);
        } catch (IOException e) {
            progressBar.abort();
            return false;
        }

        try {
            NetUtils.download(binary.url(), archivePath, progressBar::update);

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

            Path sizeFilePath = binarySizeFilePath(software);
            File sizeFile = sizeFilePath.toFile();
            if (!sizeFile.exists() && !sizeFile.createNewFile())
                throw new IOException("Failed to create .size file for binary");
            FileUtils.filePutContents(sizeFilePath, String.valueOf(archivePath.toFile().length()));

            Files.deleteIfExists(archivePath);

            try (Stream<Path> stream = Files.walk(binaryDir)) {
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

    public boolean requiresUpdateBinary(ServerSoftware software) {
        SoftwareBinary binary = (SoftwareBinary) software.binary();
        Path binaryDir = binaryDirectoryPath(software);

        try (Stream<Path> stream = Files.list(binaryDir)) {
            if (stream.count() <= 1) return true;
            if (!binary.checkForUpdates()) return !binaryDir.toFile().exists();

            Path sizeFilePath = binarySizeFilePath(software);
            File sizeFile = sizeFilePath.toFile();
            long onlineSize = cachedOnlineSizes.computeIfAbsent(binary.url(), (_) -> NetUtils.downloadSize(binary.url()));
            if (sizeFile.exists()) {
                try {
                    String content = Files.readString(sizeFile.toPath());
                    long size = Long.parseLong(content);
                    return size != onlineSize;
                } catch (Exception e) {
                    CloudLogger.get().exception("Failed to check for updates for {} for binary", e, software.name());
                    return true;
                }
            }
        } catch (IOException _) {}

        return true;
    }

    public boolean downloadBridge(ServerSoftware software) {
        SoftwareBridge bridge = (SoftwareBridge) software.bridge();
        DownloadProgressBar progressBar = createProgressBar(bridge.fileName());
        progressBar.start();

        Path sourceFile = bridgeSourceFile(software);
        Path sizeFilePath = bridgeSizeFilePath(software);

        try {
            NetUtils.download(bridge.url(), sourceFile, progressBar::update);

            File sizeFile = sizeFilePath.toFile();
            if (!sizeFile.exists() && !sizeFile.createNewFile())
                throw new IOException("Failed to create .size file for bridge");
            FileUtils.filePutContents(sizeFilePath, String.valueOf(sourceFile.toFile().length()));

            progressBar.finish();
            return true;
        } catch (Exception e) {
            progressBar.abort();
            return false;
        }
    }

    public boolean requiresUpdateBridge(ServerSoftware software) {
        SoftwareBridge bridge = (SoftwareBridge) software.bridge();
        Path sourceFile = bridgeSourceFile(software);

        if (!bridge.checkForUpdates()) return !sourceFile.toFile().exists();

        Path sizeFilePath = bridgeSizeFilePath(software);
        File sizeFile = sizeFilePath.toFile();
        long onlineSize = cachedOnlineSizes.computeIfAbsent(bridge.url(), (_) -> NetUtils.downloadSize(bridge.url()));
        if (sizeFile.exists()) {
            try {
                String content = Files.readString(sizeFile.toPath());
                long size = Long.parseLong(content);
                return size != onlineSize;
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to check for updates for {} for bridge", e, software.name());
                return true;
            }
        }

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

    public File sourceFile(ServerSoftware software) {
        return directoryPath(software).resolve(software.download().filename()).toFile();
    }

    public Path sizeFilePath(ServerSoftware software) {
        return directoryPath(software).resolve(".size");
    }

    public Path directoryPath(ServerSoftware software) {
        return PocketCloudPaths.storage().software().with(software.normalizedName()).asPath();
    }

    public Path configFilePath(ServerSoftware software) {
        return PocketCloudPaths.storage().software().with(software.normalizedName() + ".json").asPath();
    }

    public Path binaryDirectoryPath(ServerSoftware software) {
        return directoryPath(software).resolve("binary");
    }

    public Path binarySizeFilePath(ServerSoftware software) {
        return binaryDirectoryPath(software).resolve(".size");
    }

    public Path bridgeDirectoryPath(ServerSoftware software) {
        return directoryPath(software).resolve("bridge");
    }

    public Path bridgeSourceFile(ServerSoftware software) {
        return bridgeDirectoryPath(software).resolve(software.bridge().fileName());
    }

    public Path bridgeSizeFilePath(ServerSoftware software) {
        return bridgeDirectoryPath(software).resolve(".size");
    }

    private DownloadProgressBar createProgressBar(String name) {
        return new DownloadProgressBar(name, PocketCloud.instance().console().getTerminal(), PocketCloud.instance().console().getReader());
    }
}