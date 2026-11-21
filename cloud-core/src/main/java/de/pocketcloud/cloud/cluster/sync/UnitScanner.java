package de.pocketcloud.cloud.cluster.sync;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds manifests of local folders. Hashes are cached per file and only recomputed when size or
 * modification time changed.
 */
public final class UnitScanner {

    /** Size and modification time of a file; used for the stability check and to detect concurrent edits. */
    public record Stat(long size, long mtime) {
    }

    public record Scan(Manifest manifest, Map<String, Stat> listing) {
    }

    private record Cached(long size, long mtime, String sha) {
    }

    private final Map<String, Map<String, Cached>> cache = new ConcurrentHashMap<>();

    /** Manifest and listing of a unit, or null if it does not exist locally. */
    public Scan scan(SyncUnit unit) throws IOException {
        Map<String, Cached> unitCache = cache.computeIfAbsent(unit.id(), _ -> new ConcurrentHashMap<>());
        SortedMap<String, Manifest.Entry> entries = new TreeMap<>();
        Map<String, Stat> listing = new TreeMap<>();

        if (unit.meta()) {
            boolean any = false;
            for (Map.Entry<String, String> file : SyncUnit.META_FILES.entrySet()) {
                Path path = Path.of(file.getValue());
                if (!Files.isRegularFile(path)) continue;
                any = true;
                addFile(unitCache, entries, listing, file.getKey(), path, Files.readAttributes(path, BasicFileAttributes.class));
            }
            return any ? new Scan(new Manifest(entries), listing) : null;
        }

        Path root = unit.root();
        if (!Files.isDirectory(root)) return null;
        SyncIgnore ignore = SyncIgnore.load(root);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.equals(root)) return FileVisitResult.CONTINUE;
                String rel = relative(root, dir) + "/";
                if (ignore.ignored(rel, true)) return FileVisitResult.SKIP_SUBTREE;
                entries.put(rel, new Manifest.Entry(rel, 0, Manifest.DIR));
                listing.put(rel, new Stat(0, 0));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
                String rel = relative(root, file);
                if (ignore.ignored(rel, false)) return FileVisitResult.CONTINUE;
                try {
                    addFile(unitCache, entries, listing, rel, file, attrs);
                } catch (NoSuchFileException ignored) {
                    // deleted while scanning; the watcher reports it
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        unitCache.keySet().retainAll(listing.keySet());
        return new Scan(new Manifest(entries), listing);
    }

    /** Size and modification time of every replicated file (no hashing), or null if the unit does not exist. */
    public Map<String, Stat> listing(SyncUnit unit) throws IOException {
        Map<String, Stat> listing = new TreeMap<>();
        if (unit.meta()) {
            boolean any = false;
            for (Map.Entry<String, String> file : SyncUnit.META_FILES.entrySet()) {
                Path path = Path.of(file.getValue());
                if (!Files.isRegularFile(path)) continue;
                any = true;
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                listing.put(file.getKey(), new Stat(attrs.size(), attrs.lastModifiedTime().toMillis()));
            }
            return any ? listing : null;
        }

        Path root = unit.root();
        if (!Files.isDirectory(root)) return null;
        SyncIgnore ignore = SyncIgnore.load(root);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.equals(root)) return FileVisitResult.CONTINUE;
                String rel = relative(root, dir) + "/";
                if (ignore.ignored(rel, true)) return FileVisitResult.SKIP_SUBTREE;
                listing.put(rel, new Stat(0, 0));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
                String rel = relative(root, file);
                if (!ignore.ignored(rel, false)) listing.put(rel, new Stat(attrs.size(), attrs.lastModifiedTime().toMillis()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        return listing;
    }

    private void addFile(Map<String, Cached> unitCache, SortedMap<String, Manifest.Entry> entries, Map<String, Stat> listing,
                         String rel, Path file, BasicFileAttributes attrs) throws IOException {
        long size = attrs.size();
        long mtime = attrs.lastModifiedTime().toMillis();
        Cached cached = unitCache.get(rel);
        String sha;
        if (cached != null && cached.size() == size && cached.mtime() == mtime) {
            sha = cached.sha();
        } else {
            sha = sha256(file);
            unitCache.put(rel, new Cached(size, mtime, sha));
        }
        entries.put(rel, new Manifest.Entry(rel, size, sha));
        listing.put(rel, new Stat(size, mtime));
    }

    /** Remembers the hashes of files that were just written with known content (no re-hashing). */
    public void remember(SyncUnit unit, Manifest manifest) {
        Map<String, Cached> unitCache = cache.computeIfAbsent(unit.id(), _ -> new ConcurrentHashMap<>());
        unitCache.clear();
        for (Manifest.Entry e : manifest.entries().values()) {
            if (e.directory()) continue;
            Path file = unit.meta() ? Path.of(SyncUnit.META_FILES.get(e.path())) : unit.root().resolve(e.path());
            try {
                BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                if (attrs.size() == e.size()) unitCache.put(e.path(), new Cached(attrs.size(), attrs.lastModifiedTime().toMillis(), e.sha()));
            } catch (IOException ignored) {
            }
        }
    }

    public void forget(SyncUnit unit) {
        cache.remove(unit.id());
    }

    public static String relative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    public static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) > 0) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
