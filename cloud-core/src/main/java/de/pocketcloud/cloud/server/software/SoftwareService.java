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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public final class SoftwareService {

    private static final int MAX_LINK_LENGTH = 4096;
    private static final long LOOKUP_RETRY_MS = 60_000;

    private final Map<String, Long> cachedOnlineSizes = new ConcurrentHashMap<>();
    private final Map<String, Long> failedLookups = new ConcurrentHashMap<>();

    private static Path safeResolve(Path root, String name) throws IOException {
        Path target = root.resolve(name).normalize();
        if (!target.startsWith(root)) throw new IOException("Path traversal detected: " + name);
        return target;
    }

    private static void createDirsInside(Path root, Path dir) throws IOException {
        Files.createDirectories(dir);
        if (!dir.toRealPath().startsWith(root)) throw new IOException("Path escapes extraction root: " + dir);
    }

    private static void createSymlinkInside(Path root, Path link, String linkTarget) throws IOException {
        if (linkTarget.isEmpty()) throw new IOException("Empty symlink target: " + link);
        Path t = Path.of(linkTarget);
        if (t.isAbsolute()) throw new IOException("Absolute symlink target not allowed: " + linkTarget);

        boolean seenName = false;
        for (Path part : t) {
            String s = part.toString();
            if (s.equals("..")) {
                if (seenName) throw new IOException("Unsupported symlink target: " + linkTarget);
            } else if (!s.equals(".")) {
                seenName = true;
            }
        }

        Path resolved = link.getParent().toRealPath().resolve(t).normalize();
        if (!resolved.startsWith(root)) throw new IOException("Symlink escapes extraction root: " + link + " -> " + linkTarget);

        Files.deleteIfExists(link);
        Files.createSymbolicLink(link, t);
    }

    private static void createHardLinkInside(Path root, Path link, String linkName) throws IOException {
        Path source = safeResolve(root, linkName).toRealPath();
        if (!source.startsWith(root) || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid hard link: " + link + " -> " + linkName);
        Files.deleteIfExists(link);
        Files.createLink(link, source);
    }

    private static void writeFile(Path target, InputStream in) throws IOException {
        Files.deleteIfExists(target);
        try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            in.transferTo(out);
        }
    }

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
            CloudLogger.get().error("Failed to download software {}", e, software.name());
            return false;
        }
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
            CloudLogger.get().error("Failed to create directories for {} binary", e, software.name());
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
            CloudLogger.get().error("Failed to download binary for {}", e, software.name());
            return false;
        }
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
            CloudLogger.get().error("Failed to download bridge for {}", e, software.name());
            return false;
        }
    }

    public boolean requiresUpdateSoftware(ServerSoftware software) {
        SoftwareDownload download = (SoftwareDownload) software.download();
        if (!sourceFile(software).isFile()) return true;
        if (!download.checkForUpdates()) return false;

        long onlineSize = onlineSize(download.url());
        if (onlineSize <= 0) return false;
        return !sizeMatches(sizeFilePath(software), onlineSize);
    }

    public boolean requiresUpdateBridge(ServerSoftware software) {
        SoftwareBridge bridge = (SoftwareBridge) software.bridge();
        if (!Files.isRegularFile(bridgeSourceFile(software))) return true;
        if (!bridge.checkForUpdates()) return false;

        long onlineSize = onlineSize(bridge.url());
        if (onlineSize <= 0) return false;
        return !sizeMatches(bridgeSizeFilePath(software), onlineSize);
    }

    public boolean requiresUpdateBinary(ServerSoftware software) {
        SoftwareBinary binary = (SoftwareBinary) software.binary();
        if (binary.url() == null || binary.url().isBlank()) return false;

        if (!Files.isRegularFile(binarySizeFilePath(software))) return true;
        if (!binary.checkForUpdates()) return false;

        long onlineSize = onlineSize(binary.url());
        if (onlineSize <= 0) return false;
        return !sizeMatches(binarySizeFilePath(software), onlineSize);
    }

    private void extractZipEntry(ZipArchiveEntry entry, ZipArchiveInputStream zis, Path root) throws IOException {
        if (entry.getName().isEmpty()) return;
        Path target = safeResolve(root, entry.getName());

        if (entry.isDirectory()) {
            createDirsInside(root, target);
            return;
        }

        createDirsInside(root, target.getParent());

        if (entry.isUnixSymlink()) {
            byte[] raw = zis.readNBytes(MAX_LINK_LENGTH + 1);
            if (raw.length > MAX_LINK_LENGTH) throw new IOException("Symlink target too long: " + entry.getName());
            createSymlinkInside(root, target, new String(raw, StandardCharsets.UTF_8).trim());
        } else {
            writeFile(target, zis);
        }
    }

    private void extractTarEntry(TarArchiveEntry entry, TarArchiveInputStream tis, Path root) throws IOException {
        if (entry.getName().isEmpty()) return;
        Path target = safeResolve(root, entry.getName());

        if (entry.isDirectory()) {
            createDirsInside(root, target);
            return;
        }

        createDirsInside(root, target.getParent());

        if (entry.isSymbolicLink()) createSymlinkInside(root, target, entry.getLinkName());
        else if (entry.isLink()) createHardLinkInside(root, target, entry.getLinkName());
        else writeFile(target, tis);
    }

    private long onlineSize(String url) {
        Long cached = cachedOnlineSizes.get(url);
        if (cached != null) return cached;

        Long failedAt = failedLookups.get(url);
        if (failedAt != null && System.currentTimeMillis() - failedAt < LOOKUP_RETRY_MS) return -1;

        long size = NetUtils.downloadSize(url);
        if (size > 0) {
            cachedOnlineSizes.put(url, size);
            failedLookups.remove(url);
        } else {
            failedLookups.put(url, System.currentTimeMillis());
        }

        return size;
    }

    private static boolean sizeMatches(Path sizeFile, long expected) {
        try {
            return Long.parseLong(Files.readString(sizeFile).trim()) == expected;
        } catch (Exception e) {
            return false;
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