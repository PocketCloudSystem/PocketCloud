package de.pocketcloud.cloud.cluster.sync;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.pocketcloud.cloud.console.log.CloudLogger;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Head only: the last {@value #KEEP} versions of every unit, in {@code template-history/<unit>/}.
 * <p>
 * {@code versions.json} holds the version records (time, node, changed files, full manifest).
 * {@code blobs/<sha256>} holds the file contents, stored once per content hash. Every file of every kept
 * version (including the current one) has a blob, so the head can serve and restore any kept version
 * even after the template folder changed in place.
 */
public final class TemplateHistory {

    public static final int KEEP = 10;
    private static final Gson GSON = new Gson();

    public static final class Record {
        public long version;
        public long time;
        public String node;
        public String note;
        public boolean deleted;
        public List<String> changed = new ArrayList<>();
        public List<String> removed = new ArrayList<>();
        public List<String> conflicts = new ArrayList<>();
        public List<List<Object>> manifest;

        public Manifest manifest() {
            return Manifest.fromList(manifest);
        }
    }

    private final Path directory;

    public TemplateHistory(Path directory) {
        this.directory = directory;
    }

    private Path unitDir(SyncUnit unit) {
        return directory.resolve(unit.key());
    }

    public Path blob(SyncUnit unit, String sha) {
        return unitDir(unit).resolve("blobs").resolve(sha);
    }

    public boolean hasBlob(SyncUnit unit, String sha) {
        return Files.isRegularFile(blob(unit, sha));
    }

    /**
     * Copies every file of {@code manifest} that has no blob yet from the unit folder.
     *
     * @return false if a file changed since the manifest was made (hash mismatch)
     */
    public boolean ensureBlobs(SyncUnit unit, Manifest manifest, java.util.function.Function<String, Path> livePath) throws IOException {
        Path blobs = unitDir(unit).resolve("blobs");
        Files.createDirectories(blobs);
        for (Manifest.Entry e : manifest.entries().values()) {
            if (e.directory() || hasBlob(unit, e.sha())) continue;
            Path tmp = blobs.resolve(e.sha() + ".tmp");
            try {
                Files.copy(livePath.apply(e.path()), tmp, StandardCopyOption.REPLACE_EXISTING);
            } catch (NoSuchFileException ex) {
                Files.deleteIfExists(tmp);
                return false;
            }
            if (!UnitScanner.sha256(tmp).equals(e.sha())) {
                Files.deleteIfExists(tmp);
                return false;
            }
            Files.move(tmp, blob(unit, e.sha()), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        return true;
    }

    /** Adds a verified file as blob (moves it). */
    public void addBlob(SyncUnit unit, String sha, Path verifiedFile) throws IOException {
        Path target = blob(unit, sha);
        Files.createDirectories(target.getParent());
        if (Files.exists(target)) {
            Files.deleteIfExists(verifiedFile);
            return;
        }
        Files.move(verifiedFile, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public synchronized List<Record> records(SyncUnit unit) {
        Path file = unitDir(unit).resolve("versions.json");
        if (!Files.isRegularFile(file)) return new ArrayList<>();
        try {
            List<Record> list = GSON.fromJson(Files.readString(file), new TypeToken<List<Record>>() {}.getType());
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to read the history of {}", e, unit.display());
            return new ArrayList<>();
        }
    }

    public Optional<Record> record(SyncUnit unit, long version) {
        return records(unit).stream().filter(r -> r.version == version).findFirst();
    }

    /** Adds a version record, keeps the last {@value #KEEP} and removes blobs no kept version needs. */
    public synchronized void add(SyncUnit unit, Record record) throws IOException {
        List<Record> list = records(unit);
        list.removeIf(r -> r.version >= record.version);
        list.add(record);
        while (list.size() > KEEP) list.removeFirst();
        Path dir = unitDir(unit);
        Files.createDirectories(dir);
        Path tmp = dir.resolve("versions.json.tmp");
        Files.writeString(tmp, GSON.toJson(list));
        Files.move(tmp, dir.resolve("versions.json"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        Set<String> needed = new HashSet<>();
        for (Record r : list) needed.addAll(r.manifest().shas());
        Path blobs = dir.resolve("blobs");
        if (!Files.isDirectory(blobs)) return;
        try (Stream<Path> files = Files.list(blobs)) {
            for (Path blob : files.toList()) {
                if (!needed.contains(blob.getFileName().toString())) Files.deleteIfExists(blob);
            }
        }
    }
}
