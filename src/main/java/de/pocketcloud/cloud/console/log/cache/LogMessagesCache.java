package de.pocketcloud.cloud.console.log.cache;

import de.pocketcloud.cloud.console.log.CloudLogger;

import java.util.ArrayDeque;
import java.util.Deque;

public final class LogMessagesCache {

    public static final int MAX_LINES_IN_MEMORY = 100;

    private static final Deque<String> savedLines = new ArrayDeque<>();

    public static synchronized void save(String line) {
        savedLines.addLast(line);
        while (savedLines.size() > MAX_LINES_IN_MEMORY) {
            savedLines.removeFirst();
        }
    }

    public static synchronized void clear() {
        savedLines.clear();
    }

    public static synchronized void print() {
        for (String line : savedLines) {
            CloudLogger.get().echo(line);
        }
    }
}