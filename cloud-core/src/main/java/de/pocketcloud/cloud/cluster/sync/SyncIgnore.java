package de.pocketcloud.cloud.cluster.sync;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

/**
 * Which files of a replicated folder stay local.
 * <p>
 * Defaults: temporary/partial upload files, logs, caches and the log archive. A template can add
 * glob patterns in a {@code .pcignore} file (one per line, {@code #} comments):
 * <ul>
 *     <li>{@code *.bak} — no slash: matches the name of a file or folder anywhere</li>
 *     <li>{@code backups/} — trailing slash: matches folders with that name anywhere</li>
 *     <li>{@code worlds/tmp/**} — slash inside: matches the path relative to the template folder</li>
 * </ul>
 */
public final class SyncIgnore {

    public static final String FILE = ".pcignore";

    static final List<String> DEFAULTS = List.of(
            "*.tmp", "*.temp", "*.part", "*.swp", "*.swx", "~*", "*~", "*.filepart", ".filepart",
            ".DS_Store", "Thumbs.db", "*.log", "*.log.gz", "*.lck",
            "logs/", "cache/", ".cache/", "cloud_log_archive/"
    );

    private final List<PathMatcher> names = new ArrayList<>();
    private final List<PathMatcher> dirNames = new ArrayList<>();
    private final List<PathMatcher> paths = new ArrayList<>();
    private final List<String> patterns = new ArrayList<>();

    private SyncIgnore() {
    }

    public static SyncIgnore defaults() {
        SyncIgnore ignore = new SyncIgnore();
        DEFAULTS.forEach(ignore::add);
        return ignore;
    }

    /** Defaults plus the {@code .pcignore} in {@code root} (if any). */
    public static SyncIgnore load(Path root) {
        SyncIgnore ignore = defaults();
        Path file = root == null ? null : root.resolve(FILE);
        if (file != null && Files.isRegularFile(file)) {
            try {
                for (String line : Files.readAllLines(file)) {
                    line = line.strip();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    ignore.add(line);
                }
            } catch (IOException | RuntimeException ignored) {
                // unreadable or invalid pattern: keep what we have
            }
        }
        return ignore;
    }

    private void add(String pattern) {
        try {
            if (pattern.startsWith("/")) pattern = pattern.substring(1);
            if (pattern.endsWith("/") && !pattern.substring(0, pattern.length() - 1).contains("/")) {
                dirNames.add(matcher(pattern.substring(0, pattern.length() - 1)));
            } else if (pattern.contains("/")) {
                String p = pattern.endsWith("/") ? pattern + "**" : pattern;
                paths.add(matcher(p));
                if (!pattern.endsWith("/")) paths.add(matcher(p + "/**"));
                else paths.add(matcher(pattern.substring(0, pattern.length() - 1)));
            } else {
                names.add(matcher(pattern));
            }
            patterns.add(pattern);
        } catch (RuntimeException ignored) {
            // invalid glob
        }
    }

    private static PathMatcher matcher(String glob) {
        return FileSystems.getDefault().getPathMatcher("glob:" + glob);
    }

    /**
     * @param relativePath path inside the replicated folder, {@code /}-separated
     * @param directory    whether the path is a folder
     */
    public boolean ignored(String relativePath, boolean directory) {
        if (relativePath.isEmpty()) return false;
        String clean = relativePath.endsWith("/") ? relativePath.substring(0, relativePath.length() - 1) : relativePath;
        String[] parts = clean.split("/");
        for (int i = 0; i < parts.length; i++) {
            Path name = Path.of(parts[i]);
            boolean isDir = i < parts.length - 1 || directory;
            for (PathMatcher m : names) if (m.matches(name)) return true;
            if (isDir) for (PathMatcher m : dirNames) if (m.matches(name)) return true;
        }
        if (!paths.isEmpty()) {
            Path path = Path.of(clean);
            for (PathMatcher m : paths) if (m.matches(path)) return true;
        }
        return false;
    }

    public List<String> patterns() {
        return List.copyOf(patterns);
    }
}
