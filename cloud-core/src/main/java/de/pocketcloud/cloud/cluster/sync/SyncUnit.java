package de.pocketcloud.cloud.cluster.sync;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One replicated folder: {@code templates/<name>}, {@code templates/global/<type>}, {@code groups/<name>},
 * or {@link #META}: the JSON provider files that define templates and groups.
 * The id is the folder path relative to the cloud directory ({@code /}-separated).
 */
public record SyncUnit(String id) {

    public static final String META_ID = "meta";
    public static final SyncUnit META = new SyncUnit(META_ID);

    /** Files of the meta unit: name in the manifest → path relative to the cloud directory. */
    public static final Map<String, String> META_FILES;

    static {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("templates.json", "templates/templates.json");
        files.put("groups.json", "groups/groups.json");
        META_FILES = Map.copyOf(files);
    }

    public boolean meta() {
        return META_ID.equals(id);
    }

    /** Folder of the unit (relative to the cloud directory). Not defined for {@link #META}. */
    public Path root() {
        return Path.of(id);
    }

    /** Name for files on disk (history, state). */
    public String key() {
        return id.replace('/', '~');
    }

    public static SyncUnit fromKey(String key) {
        return new SyncUnit(key.replace('~', '/'));
    }

    /** Short name for logs and commands: {@code Lobby}, {@code global/server}, {@code groups/Foo}, {@code meta}. */
    public String display() {
        if (id.startsWith("templates/")) return id.substring("templates/".length());
        return id;
    }

    /** Resolves what the user typed in a command. */
    public static SyncUnit parse(String input) {
        if (input.equals(META_ID)) return META;
        if (input.startsWith("templates/") || input.startsWith("groups/")) return new SyncUnit(input);
        if (input.startsWith("group:")) return new SyncUnit("groups/" + input.substring(6));
        return new SyncUnit("templates/" + input);
    }

    /**
     * The unit a path belongs to, or null if the path is not replicated.
     *
     * @param relative path relative to the cloud directory, {@code /}-separated
     */
    public static SyncUnit of(String relative, boolean jsonMeta) {
        String[] parts = relative.split("/");
        if (parts.length < 2) return null;
        if (parts[0].equals("templates")) {
            if (parts.length == 2 && parts[1].equals("templates.json")) return jsonMeta ? META : null;
            if (parts[1].equals("global")) {
                if (parts.length < 3 || !validName(parts[2])) return null;
                return new SyncUnit("templates/global/" + parts[2]);
            }
            return validName(parts[1]) ? new SyncUnit("templates/" + parts[1]) : null;
        }
        if (parts[0].equals("groups")) {
            if (parts.length == 2 && parts[1].equals("groups.json")) return jsonMeta ? META : null;
            return validName(parts[1]) ? new SyncUnit("groups/" + parts[1]) : null;
        }
        return null;
    }

    /** Template and group names never contain dots, so files directly in templates/ or groups/ are not units. */
    public static boolean validName(String name) {
        return name.matches("[a-zA-Z0-9_\\-]+") && !name.startsWith("-");
    }

    @Override
    public String toString() {
        return id;
    }
}
