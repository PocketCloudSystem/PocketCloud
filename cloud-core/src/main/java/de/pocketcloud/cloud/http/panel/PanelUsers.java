package de.pocketcloud.cloud.http.panel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.util.PocketCloudPaths;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Panel users with permissions, stored with PBKDF2 password hashes in {@code storage/internal/panel/users.json}.
 * The auth-key login stays and counts as {@link #KEY_USER} with every permission.
 * <p>
 * shortcut: users are stored per node (like sessions); copy users.json to the other nodes, or sync it through the
 * cluster if people log in on more than one node.
 */
public final class PanelUsers {

    public static final String KEY_USER = "auth-key";
    /** Every write needs one of these; reading is allowed for every user. "admin" includes all others. */
    public static final List<String> PERMISSIONS = List.of("servers", "templates", "players", "console", "admin");

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_.-]{2,32}$");
    private static final int ITERATIONS = 210_000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Path FILE = PocketCloudPaths.storage().internal().with("panel").with("users.json").asPath();

    public record User(String name, String salt, String hash, Set<String> permissions) {
        public boolean may(String permission) {
            return permissions.contains("admin") || permissions.contains(permission);
        }
    }

    private static Map<String, User> users;

    private PanelUsers() {
    }

    public static synchronized Optional<User> get(String name) {
        if (KEY_USER.equals(name)) return Optional.of(new User(KEY_USER, "", "", Set.of("admin")));
        return Optional.ofNullable(load().get(name.toLowerCase(Locale.ROOT)));
    }

    public static synchronized List<User> list() {
        return load().values().stream().sorted(Comparator.comparing(User::name)).toList();
    }

    /** @return the user if the password is right; takes about as long for unknown users (no name probing) */
    public static Optional<User> check(String name, String password) {
        User user = get(name).filter(u -> !u.name().equals(KEY_USER)).orElse(null);
        byte[] salt = user != null ? HexFormat.of().parseHex(user.salt()) : new byte[16];
        byte[] hash = hash(password, salt);
        boolean ok = user != null && MessageDigest.isEqual(hash, HexFormat.of().parseHex(user.hash()));
        return ok ? Optional.of(user) : Optional.empty();
    }

    /** Creates or updates a user; a null password keeps the old one. */
    public static synchronized User save(String name, String password, Collection<String> permissions) {
        if (name == null || !NAME.matcher(name).matches() || name.equalsIgnoreCase(KEY_USER))
            throw new HttpException(400, "Names have 2-32 letters, digits, _ . or - (and cannot be auth-key).");
        for (String p : permissions) if (!PERMISSIONS.contains(p)) throw new HttpException(400, "Unknown permission: " + p);
        User old = load().get(name.toLowerCase(Locale.ROOT));
        String salt, hash;
        if (password == null || password.isEmpty()) {
            if (old == null) throw new HttpException(400, "New users need a password.");
            salt = old.salt();
            hash = old.hash();
        } else {
            if (password.length() < 8) throw new HttpException(400, "Passwords need at least 8 characters.");
            byte[] bytes = new byte[16];
            RANDOM.nextBytes(bytes);
            salt = HexFormat.of().formatHex(bytes);
            hash = HexFormat.of().formatHex(hash(password, bytes));
        }
        User user = new User(old != null ? old.name() : name, salt, hash, new TreeSet<>(permissions));
        users.put(name.toLowerCase(Locale.ROOT), user);
        write();
        return user;
    }

    public static synchronized boolean remove(String name) {
        boolean removed = load().remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) write();
        return removed;
    }

    private static Map<String, User> load() {
        if (users != null) return users;
        users = new LinkedHashMap<>();
        if (Files.exists(FILE)) {
            try {
                List<User> list = GSON.fromJson(Files.readString(FILE), new TypeToken<List<User>>() {}.getType());
                if (list != null) for (User u : list) users.put(u.name().toLowerCase(Locale.ROOT), u);
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException("Could not read " + FILE + ": " + e.getMessage(), e);
            }
        }
        return users;
    }

    private static void write() {
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling("users.json.tmp");
            Files.writeString(tmp, GSON.toJson(users.values()));
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new HttpException(500, "Could not save the users: " + e.getMessage());
        }
    }

    private static byte[] hash(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 is not available", e);
        }
    }

    /** Permission a write request needs, from its path (prefix and API version stripped). */
    static String required(String path) {
        String p = path.replaceFirst("^.*?/(v\\d+|panel-api)/", "/");
        if (p.startsWith("/servers")) return "servers";
        if (p.startsWith("/templates") || p.startsWith("/groups") || p.startsWith("/maintenance")) return "templates";
        if (p.startsWith("/players")) return "players";
        if (p.equals("/console")) return "console";
        return "admin";
    }
}
