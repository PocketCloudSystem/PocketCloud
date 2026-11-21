package de.pocketcloud.cloud.cluster;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.common.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The member list of the cluster ({@code storage/internal/cluster/members.json}).
 * <p>
 * Changed only by {@code node add} / {@code node remove}. Every change gets a higher version,
 * and nodes adopt any list with a higher version they receive (hello, heartbeat), so changes
 * spread through the whole cluster within a second. Only members may connect to each other,
 * which also makes {@code node remove} the revocation of a node.
 */
public final class ClusterMembers {

    public record Member(String name, String address, int port) {
        public String endpoint() {
            return address + ":" + port;
        }
    }

    private final Path file;
    private long version = 0;
    private final Map<String, Member> members = new TreeMap<>();

    public ClusterMembers(Path file) {
        this.file = file;
    }

    public synchronized void load() throws IOException {
        if (!Files.isRegularFile(file)) return;
        Map<String, Object> data = FileUtils.GSON.fromJson(Files.readString(file), new TypeToken<Map<String, Object>>() {}.getType());
        if (data != null) apply(data);
    }

    public synchronized void save() throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, FileUtils.PRETTY_GSON.toJson(write()));
    }

    public synchronized long version() {
        return version;
    }

    public synchronized boolean contains(String name) {
        return members.containsKey(name);
    }

    public synchronized Optional<Member> get(String name) {
        return Optional.ofNullable(members.get(name));
    }

    public synchronized List<Member> all() {
        return List.copyOf(members.values());
    }

    public synchronized int size() {
        return members.size();
    }

    /** Adds or updates a member and bumps the version. */
    public synchronized void put(Member member) throws IOException {
        members.put(member.name(), member);
        bump();
    }

    public synchronized boolean remove(String name) throws IOException {
        if (members.remove(name) == null) return false;
        bump();
        return true;
    }

    /**
     * Takes over a received member list if it is newer.
     *
     * @return true if the list changed
     */
    public synchronized boolean adoptIfNewer(Map<String, Object> data) throws IOException {
        if (data == null || data.isEmpty()) return false;
        Object v = data.get("version");
        if (!(v instanceof Number number) || number.longValue() <= version) return false;
        apply(data);
        save();
        return true;
    }

    public synchronized Map<String, Object> write() {
        Map<String, Object> nodes = new LinkedHashMap<>();
        for (Member m : members.values()) nodes.put(m.name(), Map.of("address", m.address(), "port", m.port()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("version", version);
        out.put("nodes", nodes);
        return out;
    }

    @SuppressWarnings("unchecked")
    private void apply(Map<String, Object> data) {
        version = ((Number) data.getOrDefault("version", 0L)).longValue();
        members.clear();
        Object nodes = data.get("nodes");
        if (nodes instanceof Map<?, ?> map) {
            map.forEach((name, value) -> {
                Map<String, Object> m = (Map<String, Object>) value;
                members.put(name.toString(), new Member(name.toString(), String.valueOf(m.get("address")), ((Number) m.get("port")).intValue()));
            });
        }
    }

    private void bump() throws IOException {
        version = Math.max(version + 1, System.currentTimeMillis());
        save();
    }
}
