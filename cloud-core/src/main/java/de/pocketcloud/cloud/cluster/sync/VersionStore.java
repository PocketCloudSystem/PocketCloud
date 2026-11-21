package de.pocketcloud.cloud.cluster.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.pocketcloud.cloud.console.log.CloudLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unit versions persisted in one JSON file ({@code head.json} for the head's valid versions,
 * {@code applied.json} for what a node last took over from the head).
 */
public final class VersionStore {

    private static final Gson GSON = new GsonBuilder().create();

    private static final class Stored {
        long version;
        boolean deleted;
        List<List<Object>> manifest;
    }

    private final Path file;
    private final Map<String, UnitVersion> versions = new ConcurrentHashMap<>();

    public VersionStore(Path file) {
        this.file = file;
    }

    public synchronized void load() {
        versions.clear();
        if (!Files.isRegularFile(file)) return;
        try {
            Map<String, Stored> data = GSON.fromJson(Files.readString(file), new TypeToken<Map<String, Stored>>() {}.getType());
            if (data == null) return;
            data.forEach((id, s) -> versions.put(id, new UnitVersion(s.version, Manifest.fromList(s.manifest), s.deleted)));
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to read {}", e, file);
        }
    }

    public UnitVersion get(SyncUnit unit) {
        return versions.get(unit.id());
    }

    public Map<String, UnitVersion> all() {
        return new TreeMap<>(versions);
    }

    public synchronized void put(SyncUnit unit, UnitVersion version) {
        versions.put(unit.id(), version);
        save();
    }

    public synchronized void remove(SyncUnit unit) {
        if (versions.remove(unit.id()) != null) save();
    }

    private void save() {
        Map<String, Stored> data = new TreeMap<>();
        versions.forEach((id, v) -> {
            Stored s = new Stored();
            s.version = v.version();
            s.deleted = v.deleted();
            s.manifest = v.manifest().toList();
            data.put(id, s);
        });
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(data));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to write {}", e, file);
        }
    }
}
