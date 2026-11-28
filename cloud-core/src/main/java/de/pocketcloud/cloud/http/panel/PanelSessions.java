package de.pocketcloud.cloud.http.panel;

import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logins of the web panel: the browser sends a user's password once and gets a random session id as HttpOnly cookie,
 * so the key itself is never stored in the browser. Sessions live in memory of this node (a restart logs out).
 */
public final class PanelSessions {

    public static final String COOKIE = "pocketcloud_session";
    /** Header the panel sends with every write; a foreign site cannot set it without a CORS preflight (CSRF). */
    public static final String CSRF_HEADER = "X-PocketCloud-Panel";
    public static final Duration LIFETIME = Duration.ofHours(12);

    private static final int MAX_FAILURES = 5;
    private static final long FAILURE_WINDOW_MS = 60_000;

    private static final SecureRandom RANDOM = new SecureRandom();
    private record Session(String user, long expires) {
    }

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    /** remote address → [failures, window start] */
    private static final Map<String, long[]> failures = new ConcurrentHashMap<>();

    private PanelSessions() {
    }

    public static String create(String user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String id = HexFormat.of().formatHex(bytes);
        long now = System.currentTimeMillis();
        sessions.values().removeIf(s -> s.expires() < now);
        sessions.put(id, new Session(user, now + LIFETIME.toMillis()));
        return id;
    }

    public static void remove(HttpRequest request) {
        String id = sessionId(request);
        if (id != null) sessions.remove(id);
    }

    /** Logs out every session of a user (deleted, or permissions taken away). */
    public static void removeUser(String user) {
        sessions.values().removeIf(s -> s.user().equalsIgnoreCase(user));
    }

    /** The logged-in user of a valid session, if any. */
    public static Optional<PanelUsers.User> user(HttpRequest request) {
        String id = sessionId(request);
        Session session = id == null ? null : sessions.get(id);
        if (session == null || session.expires() < System.currentTimeMillis()) return Optional.empty();
        return PanelUsers.get(session.user());
    }

    /**
     * Valid session cookie; writes also need the panel header and the permission for the route (403 otherwise).
     * The user management needs admin for reading too.
     */
    public static boolean authenticated(HttpRequest request) {
        PanelUsers.User user = user(request).orElse(null);
        if (user == null) return false;
        String method = request.method().name();
        boolean read = method.equals("GET") || method.equals("HEAD");
        if (!read && request.header(CSRF_HEADER) == null) return false;
        String needed = read ? (request.path().endsWith("/panel-api/users") ? "admin" : null) : PanelUsers.required(request.path());
        if (needed != null && !user.may(needed)) throw new HttpException(403, "You need the permission \"" + needed + "\".");
        return true;
    }

    private static String sessionId(HttpRequest request) {
        String header = request.header("Cookie");
        if (header == null) return null;
        for (Cookie cookie : ServerCookieDecoder.STRICT.decode(header)) {
            if (cookie.name().equals(COOKIE)) return cookie.value();
        }
        return null;
    }

    /** @return false if this address had too many failed logins in the last minute */
    public static boolean mayTry(String address) {
        long[] entry = failures.get(address);
        if (entry == null || System.currentTimeMillis() - entry[1] > FAILURE_WINDOW_MS) return true;
        return entry[0] < MAX_FAILURES;
    }

    public static void failed(String address) {
        long now = System.currentTimeMillis();
        failures.compute(address, (_, e) -> e == null || now - e[1] > FAILURE_WINDOW_MS ? new long[]{1, now} : new long[]{e[0] + 1, e[1]});
    }

    public static void succeeded(String address) {
        failures.remove(address);
    }
}
