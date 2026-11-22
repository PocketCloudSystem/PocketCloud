package de.pocketcloud.cloud.cluster.scheduling;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.common.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * The nodes in drain mode ({@code storage/internal/cluster/drain.json}).
 * <p>
 * Only the head changes the set; every change gets a higher version. All nodes carry the set in their
 * heartbeats and take over any newer version they receive, so every node (and therefore any future head)
 * knows it and keeps it across restarts.
 */
public final class DrainState {

    private final Path file;
    private long version;
    private final Set<String> nodes = new TreeSet<>();

    public DrainState(Path file) {
        this.file = file;
    }

    public synchronized void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            Map<String, Object> data = FileUtils.GSON.fromJson(Files.readString(file), new TypeToken<Map<String, Object>>() {}.getType());
            if (data != null) apply(data);
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to read {}", e, file);
        }
    }

    public synchronized boolean contains(String node) {
        return nodes.contains(node);
    }

    public synchronized Set<String> nodes() {
        return Set.copyOf(nodes);
    }

    public synchronized long version() {
        return version;
    }

    /**
     * Head: sets or clears the drain flag of a node.
     *
     * @return false if nothing changed
     */
    public synchronized boolean set(String node, boolean drain) {
        boolean changed = drain ? nodes.add(node) : nodes.remove(node);
        if (!changed) return false;
        version = Math.max(version + 1, System.currentTimeMillis());
        save();
        return true;
    }

    /** Takes over a received state if it is newer. Returns true if it changed. */
    public synchronized boolean adoptIfNewer(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) return false;
        Object v = map.get("version");
        if (!(v instanceof Number number) || number.longValue() <= version) return false;
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) map;
        apply(data);
        save();
        return true;
    }

    public synchronized Map<String, Object> write() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("version", version);
        out.put("nodes", new ArrayList<>(nodes));
        return out;
    }

    private void apply(Map<String, Object> data) {
        version = data.get("version") instanceof Number n ? n.longValue() : 0;
        nodes.clear();
        if (data.get("nodes") instanceof Collection<?> list) list.forEach(n -> nodes.add(String.valueOf(n)));
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, FileUtils.PRETTY_GSON.toJson(write()));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to write {}", e, file);
        }
    }
}
