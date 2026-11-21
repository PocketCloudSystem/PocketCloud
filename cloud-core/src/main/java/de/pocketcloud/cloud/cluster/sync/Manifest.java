package de.pocketcloud.cloud.cluster.sync;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Files of one replicated folder: sorted relative path → (size, SHA-256).
 * Directories are listed with a trailing {@code /} and the hash {@value #DIR}, so empty folders survive.
 * The manifest hash is the SHA-256 over all entries; equal hashes mean identical content.
 */
public final class Manifest {

    public static final String DIR = "dir";
    public static final Manifest EMPTY = new Manifest(new TreeMap<>());
    private static final Gson GSON = new Gson();

    public record Entry(String path, long size, String sha) {
        public boolean directory() {
            return path.endsWith("/");
        }
    }

    private final SortedMap<String, Entry> entries;
    private String hash;

    public Manifest(SortedMap<String, Entry> entries) {
        this.entries = Collections.unmodifiableSortedMap(new TreeMap<>(entries));
    }

    public SortedMap<String, Entry> entries() {
        return entries;
    }

    public Entry get(String path) {
        return entries.get(path);
    }

    public int fileCount() {
        return (int) entries.values().stream().filter(e -> !e.directory()).count();
    }

    public long totalSize() {
        return entries.values().stream().mapToLong(Entry::size).sum();
    }

    /** Content hashes of all files. */
    public Set<String> shas() {
        Set<String> set = new HashSet<>();
        for (Entry e : entries.values()) if (!e.directory()) set.add(e.sha());
        return set;
    }

    /** Map from content hash to one path that has it (files only). */
    public Map<String, String> pathsBySha() {
        Map<String, String> map = new HashMap<>();
        for (Entry e : entries.values()) if (!e.directory()) map.putIfAbsent(e.sha(), e.path());
        return map;
    }

    public String hash() {
        if (hash == null) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                for (Entry e : entries.values()) {
                    digest.update((e.path() + '\t' + e.size() + '\t' + e.sha() + '\n').getBytes(StandardCharsets.UTF_8));
                }
                hash = HexFormat.of().formatHex(digest.digest());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return hash;
    }

    public static String shortHash(String hash) {
        return hash == null ? "-" : hash.substring(0, Math.min(10, hash.length()));
    }

    // ------------------------------------------------------------------ diff

    /** Paths that are new or changed in {@code to} compared to {@code from}. */
    public static List<String> changed(Manifest from, Manifest to) {
        List<String> list = new ArrayList<>();
        for (Entry e : to.entries.values()) {
            Entry old = from == null ? null : from.get(e.path());
            if (old == null || !old.sha().equals(e.sha()) || old.size() != e.size()) list.add(e.path());
        }
        return list;
    }

    /** Paths of {@code from} that are gone in {@code to}. */
    public static List<String> removed(Manifest from, Manifest to) {
        if (from == null) return List.of();
        List<String> list = new ArrayList<>();
        for (String path : from.entries.keySet()) if (to == null || !to.entries.containsKey(path)) list.add(path);
        return list;
    }

    // ------------------------------------------------------------------ serialization

    public List<List<Object>> toList() {
        List<List<Object>> list = new ArrayList<>(entries.size());
        for (Entry e : entries.values()) list.add(List.of(e.path(), e.size(), e.sha()));
        return list;
    }

    public static Manifest fromList(List<List<Object>> list) {
        SortedMap<String, Entry> map = new TreeMap<>();
        if (list != null) {
            for (List<Object> row : list) {
                String path = String.valueOf(row.get(0));
                long size = ((Number) row.get(1)).longValue();
                map.put(path, new Entry(path, size, String.valueOf(row.get(2))));
            }
        }
        return new Manifest(map);
    }

    public String toJson() {
        return GSON.toJson(toList());
    }

    public static Manifest fromJson(String json) {
        List<List<Object>> list = GSON.fromJson(json, new TypeToken<List<List<Object>>>() {}.getType());
        return fromList(list);
    }

    /** Gzip + Base64, for packets. */
    public String encode() {
        return compress(toJson());
    }

    public static Manifest decode(String data) {
        return fromJson(decompress(data));
    }

    public static String compress(String text) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                gzip.write(text.getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String decompress(String data) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
