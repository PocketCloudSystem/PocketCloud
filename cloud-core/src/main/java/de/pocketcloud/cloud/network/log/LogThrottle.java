package de.pocketcloud.cloud.network.log;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LogThrottle {

    private static final class State {

        long windowStart;
        int logged;
        int suppressed;
    }

    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final int maxPerWindow;
    private final long windowMillis;

    public LogThrottle(int maxPerWindow, Duration window) {
        this.maxPerWindow = maxPerWindow;
        this.windowMillis = window.toMillis();
    }

    public int acquire(String key) {
        State s = states.computeIfAbsent(key, _ -> new State());
        synchronized (s) {
            long now = System.currentTimeMillis();
            if (now - s.windowStart >= windowMillis) {
                int previouslySuppressed = s.suppressed;
                s.windowStart = now;
                s.logged = 1;
                s.suppressed = 0;
                return previouslySuppressed;
            }

            if (s.logged < maxPerWindow) {
                s.logged++;
                return 0;
            }

            s.suppressed++;
            return -1;
        }
    }
}