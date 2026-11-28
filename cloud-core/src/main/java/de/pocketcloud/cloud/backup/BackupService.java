package de.pocketcloud.cloud.backup;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.storage.LocalTemplateStorage;
import de.pocketcloud.cloud.cluster.sync.S3BlobStore;
import de.pocketcloud.cloud.config.sub.BackupConfiguration;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.util.FileUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip backups of templates, groups, configs and in-game files in {@code storage/backups/}.
 * Not included: node.yml (identity of the node), storage/internal (certificates, keys, cluster state), template log archives.
 * A restore replaces templates/ and groups/ (the template sync then distributes them in a cluster) and overwrites the
 * config and in-game files; a backup of the current state is made first.
 */
public final class BackupService {

    public static final String AUTO = "auto";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Set<String> SKIPPED = Set.of("node.yml", LocalTemplateStorage.LOG_ARCHIVE);

    private static ScheduledExecutorService scheduler;

    private BackupService() {
    }

    /** Entry name in the zip → folder on disk. */
    private static Map<String, Path> roots() {
        Map<String, Path> roots = new LinkedHashMap<>();
        roots.put("templates", PocketCloudPaths.templates().asPath());
        roots.put("groups", PocketCloudPaths.groups().asPath());
        roots.put("configs", PocketCloudPaths.storage().configs().asPath());
        roots.put("inGame", PocketCloudPaths.storage().inGame().asPath());
        return roots;
    }

    private static Path directory() {
        return PocketCloudPaths.storage().backups().asPath();
    }

    /** Creates {@code <kind>-<time>.zip} and returns its path. */
    public static synchronized Path create(String kind) throws IOException {
        Path dir = directory();
        Files.createDirectories(dir);
        Path zip = dir.resolve(kind + "-" + LocalDateTime.now().format(STAMP) + ".zip");
        Path tmp = dir.resolve(zip.getFileName() + ".tmp");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(tmp))) {
            for (Map.Entry<String, Path> root : roots().entrySet()) {
                if (!Files.isDirectory(root.getValue())) continue;
                try (Stream<Path> files = Files.walk(root.getValue())) {
                    for (Path file : (Iterable<Path>) files::iterator) {
                        if (!Files.isRegularFile(file) || skipped(root.getValue().relativize(file))) continue;
                        out.putNextEntry(new ZipEntry(root.getKey() + "/" + root.getValue().relativize(file).toString().replace('\\', '/')));
                        Files.copy(file, out);
                        out.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
        Files.move(tmp, zip, StandardCopyOption.ATOMIC_MOVE);
        uploadToS3(zip);
        return zip;
    }

    private static boolean skipped(Path relative) {
        for (Path part : relative) if (SKIPPED.contains(part.toString())) return true;
        return false;
    }

    /** Backup zips, newest first. */
    public static List<Path> list() throws IOException {
        if (!Files.isDirectory(directory())) return List.of();
        try (Stream<Path> files = Files.list(directory())) {
            return files.filter(p -> p.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparing((Path p) -> p.toFile().lastModified()).reversed()).toList();
        }
    }

    /**
     * Restores a backup by file name. Makes a {@code before-restore} backup first.
     *
     * @return the name of the backup of the previous state
     */
    public static synchronized String restore(String name) throws IOException {
        if (!name.matches("[A-Za-z0-9._-]+\\.zip")) throw new IllegalArgumentException("invalid backup name");
        Path zip = directory().resolve(name);
        if (!Files.isRegularFile(zip)) throw new NoSuchFileException("backup " + name + " does not exist");

        Path before = create("before-restore");
        Path staging = directory().resolve(".restore-" + System.nanoTime());
        try {
            Set<String> present = extract(zip, staging);
            for (Map.Entry<String, Path> root : roots().entrySet()) {
                if (!present.contains(root.getKey())) continue;
                Path source = staging.resolve(root.getKey());
                Path target = root.getValue();
                if (root.getKey().equals("templates") || root.getKey().equals("groups")) {
                    replaceContents(source, target);
                } else {
                    FileUtils.copyDirectory(source, target, Set.of()); // overwrite, keep files the backup does not have (node.yml)
                }
            }
        } finally {
            FileUtils.removeDirectory(staging);
        }
        return before.getFileName().toString();
    }

    /** Extracts the zip into {@code staging} and returns the top-level folders it contained. */
    private static Set<String> extract(Path zip, Path staging) throws IOException {
        Set<String> present = new HashSet<>();
        Path base = staging.toAbsolutePath().normalize();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                Path target = base.resolve(entry.getName()).normalize();
                String top = entry.getName().split("/", 2)[0];
                if (!target.startsWith(base) || !roots().containsKey(top)) throw new IOException("invalid entry in backup: " + entry.getName());
                present.add(top);
                if (entry.isDirectory()) continue;
                Files.createDirectories(target.getParent());
                try (OutputStream out = Files.newOutputStream(target)) {
                    in.transferTo(out);
                }
            }
        }
        return present;
    }

    /** Makes {@code target} contain exactly the files of {@code source} (template log archives stay). */
    private static void replaceContents(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> files = Files.walk(target)) {
            List<Path> existing = files.filter(Files::isRegularFile).toList();
            for (Path file : existing) {
                Path relative = target.relativize(file);
                if (!skipped(relative) && !Files.exists(source.resolve(relative))) Files.delete(file);
            }
        }
        FileUtils.copyDirectory(source, target, Set.of());
    }

    // ------------------------------------------------------------------ automatic backups

    public static void startSchedule() {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("backups").factory());
        scheduler.scheduleWithFixedDelay(BackupService::autoBackup, 1, 10, TimeUnit.MINUTES);
    }

    public static void stopSchedule() {
        if (scheduler != null) scheduler.shutdownNow();
        scheduler = null;
    }

    private static void autoBackup() {
        try {
            BackupConfiguration config = PocketCloud.instance().config().backups();
            if (config.intervalHours() <= 0) return;
            ClusterManager cluster = PocketCloud.instance().cluster();
            if (cluster.clustered() && !cluster.isHead()) return;
            List<Path> autos = list().stream().filter(p -> p.getFileName().toString().startsWith(AUTO + "-")).toList();
            long newest = autos.isEmpty() ? 0 : autos.getFirst().toFile().lastModified();
            if (System.currentTimeMillis() - newest < TimeUnit.HOURS.toMillis(config.intervalHours())) return;
            Path zip = create(AUTO);
            CloudLogger.get().info("Created the automatic backup §b{}§r.", zip.getFileName());
            for (Path old : list().stream().filter(p -> p.getFileName().toString().startsWith(AUTO + "-")).skip(config.keep()).toList()) {
                Files.deleteIfExists(old);
            }
        } catch (Exception e) {
            CloudLogger.get().warn("Could not create the automatic backup§8: §c{}", e.getMessage());
        }
    }

    private static void uploadToS3(Path zip) {
        var main = PocketCloud.instance().config();
        if (!main.backups().uploadToS3()) return;
        S3BlobStore s3 = S3BlobStore.fromConfig("s3", main.cluster().s3(), PocketCloudPaths.storage().internal().with("cluster").with("s3.properties").asPath());
        if (s3 == null) return;
        try {
            s3.putFile("backups/" + zip.getFileName(), zip);
            CloudLogger.get().debug("Uploaded the backup {} to S3", zip.getFileName());
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            CloudLogger.get().warn("Could not upload the backup §b{} §rto S3§8: §c{}", zip.getFileName(), e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
}
