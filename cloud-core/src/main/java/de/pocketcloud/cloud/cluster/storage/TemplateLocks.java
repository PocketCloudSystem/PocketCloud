package de.pocketcloud.cloud.cluster.storage;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * One read/write lock per template folder. Copying a template into a server (and saves into it)
 * takes the read lock, the template replication takes the write lock while it swaps in a new
 * version. A starting server therefore copies either the old or the new version, never a mix.
 */
public final class TemplateLocks {

    private static final Map<Path, ReadWriteLock> LOCKS = new ConcurrentHashMap<>();

    private TemplateLocks() {
    }

    public static ReadWriteLock of(Path folder) {
        return LOCKS.computeIfAbsent(folder.toAbsolutePath().normalize(), _ -> new ReentrantReadWriteLock());
    }

    public static <E extends Exception> void read(Path folder, IoRunnable<E> action) throws E {
        ReadWriteLock lock = of(folder);
        lock.readLock().lock();
        try {
            action.run();
        } finally {
            lock.readLock().unlock();
        }
    }

    @FunctionalInterface
    public interface IoRunnable<E extends Exception> {
        void run() throws E;
    }
}
