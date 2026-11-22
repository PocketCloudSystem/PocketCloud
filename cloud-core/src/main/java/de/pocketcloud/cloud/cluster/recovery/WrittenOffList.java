package de.pocketcloud.cloud.cluster.recovery;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.common.util.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Servers that were written off because their node was gone ({@code storage/internal/cluster/written-off.json}).
 * <p>
 * Every node records them when it sees a node die, keeps them for {@link #KEEP_MS} and carries the list in its
 * heartbeat; receivers merge it (union, newest time wins). So a restarted head, or a node that was not running when
 * the server was written off and becomes head later, still knows them and orders such servers to be killed when
 * their node comes back.
 */
final class WrittenOffList {

    static final long KEEP_MS = 30 * 60_000;

    record Entry(String name, String node, long time) {}

    private final Path file;
    private final Map<String, Entry> entries = new HashMap<>(); // uuid → entry
    private boolean dirty;

    WrittenOffList(Path file) {
        this.file = file;
    }

    synchronized void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            Map<String, Object> root = FileUtils.decodeJsonFile(file, new TypeToken<Map<String, Object>>() {});
            if (root != null) merge(root.get("servers"));
            expire(System.currentTimeMillis());
            dirty = false;
            if (!entries.isEmpty()) {
                CloudLogger.get().debug("Loaded {} written-off server(s) from written-off.json: {}", entries.size(), names());
            }
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to read {}", e, file.toString());
        }
    }

    synchronized boolean contains(String uuid) {
        Entry entry = entries.get(uuid);
        return entry != null && System.currentTimeMillis() - entry.time() <= KEEP_MS;
    }

    synchronized Entry get(String uuid) {
        return entries.get(uuid);
    }

    synchronized void add(String uuid, String name, String node, long time) {
        Entry old = entries.get(uuid);
        if (old != null && old.time() >= time) return;
        entries.put(uuid, new Entry(name, node, time));
        dirty = true;
    }

    synchronized void expire(long now) {
        if (entries.values().removeIf(e -> now - e.time() > KEEP_MS)) dirty = true;
    }

    synchronized int size() {
        return entries.size();
    }

    /** For the heartbeat: uuid → [name, node, time]. */
    synchronized Map<String, Object> write() {
        Map<String, Object> out = new LinkedHashMap<>();
        entries.forEach((uuid, e) -> out.put(uuid, List.of(e.name(), e.node(), e.time())));
        return out;
    }

    /**
     * Merges a list received in a heartbeat (or read from the file).
     *
     * @return names of servers this node did not know as written off yet
     */
    synchronized List<String> merge(Object raw) {
        List<String> added = new ArrayList<>();
        if (!(raw instanceof Map<?, ?> map)) return added;
        long now = System.currentTimeMillis();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getValue() instanceof List<?> values) || values.size() < 3 || !(values.get(2) instanceof Number time)) continue;
            if (now - time.longValue() > KEEP_MS) continue;
            String uuid = String.valueOf(e.getKey());
            Entry old = entries.get(uuid);
            if (old != null && old.time() >= time.longValue()) continue;
            entries.put(uuid, new Entry(String.valueOf(values.get(0)), String.valueOf(values.get(1)), time.longValue()));
            dirty = true;
            if (old == null) added.add(String.valueOf(values.get(0)));
        }
        return added;
    }

    synchronized void flush() {
        if (!dirty) return;
        dirty = false;
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("keepMinutes", KEEP_MS / 60_000);
            root.put("servers", write());
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, FileUtils.PRETTY_GSON.toJson(root));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            dirty = true;
            CloudLogger.get().exception("Failed to write {}", e, file.toString());
        }
    }

    private String names() {
        return String.join(", ", entries.values().stream().map(e -> e.name() + "@" + e.node()).toList());
    }
}
