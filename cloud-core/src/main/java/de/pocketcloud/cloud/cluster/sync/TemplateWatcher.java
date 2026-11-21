package de.pocketcloud.cloud.cluster.sync;

import de.pocketcloud.cloud.console.log.CloudLogger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Watches {@code templates/} and {@code groups/} (every sub folder registered on its own, as inotify needs it)
 * and reports the unit a changed path belongs to. Ignored folders (logs, caches, log archive) are not watched.
 */
final class TemplateWatcher implements Runnable {

    private final Path[] roots;
    private final boolean jsonMeta;
    private final Consumer<SyncUnit> onChange;
    private final Runnable onOverflow;
    private final Map<WatchKey, Path> keys = new ConcurrentHashMap<>();
    private WatchService service;
    private Thread thread;

    TemplateWatcher(boolean jsonMeta, Consumer<SyncUnit> onChange, Runnable onOverflow, Path... roots) {
        this.roots = roots;
        this.jsonMeta = jsonMeta;
        this.onChange = onChange;
        this.onOverflow = onOverflow;
    }

    void start() throws IOException {
        service = FileSystems.getDefault().newWatchService();
        for (Path root : roots) {
            Files.createDirectories(root);
            registerTree(root);
        }
        thread = new Thread(this, "PocketCloud-TemplateWatch");
        thread.setDaemon(true);
        thread.start();
    }

    void stop() {
        if (thread != null) thread.interrupt();
        try {
            if (service != null) service.close();
        } catch (IOException ignored) {
        }
    }

    int watchedFolders() {
        return keys.size();
    }

    private void registerTree(Path start) {
        try {
            Files.walkFileTree(start, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(start) || !isRoot(dir)) {
                        if (ignoredDir(dir)) return FileVisitResult.SKIP_SUBTREE;
                    }
                    keys.put(dir.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            CloudLogger.get().debug("Cannot watch {}: {}", start, e.getMessage());
        }
    }

    private boolean isRoot(Path dir) {
        for (Path root : roots) if (root.equals(dir)) return true;
        return false;
    }

    /** Folders inside a unit that the ignore rules exclude are not watched. */
    private boolean ignoredDir(Path dir) {
        String rel = relative(dir);
        SyncUnit unit = SyncUnit.of(rel, jsonMeta);
        if (unit == null || unit.meta()) return false;
        String root = unit.id();
        if (rel.length() <= root.length()) return false;
        String inner = rel.substring(root.length() + 1);
        return ignore(unit).ignored(inner, true);
    }

    private record CachedIgnore(SyncIgnore ignore, long loadedAt) {
    }

    private final Map<String, CachedIgnore> ignores = new ConcurrentHashMap<>();

    /** Ignore rules of a unit, re-read at most every 2 seconds. */
    private SyncIgnore ignore(SyncUnit unit) {
        long now = System.currentTimeMillis();
        CachedIgnore cached = ignores.get(unit.id());
        if (cached == null || now - cached.loadedAt() > 2_000) {
            cached = new CachedIgnore(SyncIgnore.load(unit.root()), now);
            ignores.put(unit.id(), cached);
        }
        return cached.ignore();
    }

    private static String relative(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = service.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                return;
            }
            Path dir = keys.get(key);
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == OVERFLOW) {
                    onOverflow.run();
                    continue;
                }
                if (dir == null) continue;
                Path child = dir.resolve((Path) event.context());
                String rel = relative(child);
                if (event.kind() == ENTRY_CREATE && Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    registerTree(child);
                }
                boolean directory = Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS);
                SyncUnit unit = SyncUnit.of(rel, jsonMeta);
                if (unit == null) continue;
                if (!unit.meta() && rel.length() > unit.id().length()) {
                    String inner = rel.substring(unit.id().length() + 1);
                    if (ignore(unit).ignored(inner, directory)) continue;
                }
                onChange.accept(unit);
            }
            if (!key.reset()) keys.remove(key);
        }
    }
}
