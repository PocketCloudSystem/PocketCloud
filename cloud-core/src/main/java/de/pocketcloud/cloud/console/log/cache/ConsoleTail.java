package de.pocketcloud.cloud.console.log.cache;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** The last console lines (with ANSI colours) for the web panel; readers poll with the last sequence number they saw. */
public final class ConsoleTail {

    public static final int MAX_LINES = 1000;

    public record Line(long seq, String text) {}

    private static final ArrayDeque<Line> LINES = new ArrayDeque<>();
    private static long nextSeq = 1;

    private ConsoleTail() {}

    public static synchronized void add(String formatted) {
        for (String text : formatted.split("\\R")) {
            if (text.replaceAll("\u001B\\[[0-9;]*m", "").isBlank()) continue;
            LINES.addLast(new Line(nextSeq++, text));
            if (LINES.size() > MAX_LINES) LINES.removeFirst();
        }
    }

    /** Lines after {@code seq}; 0 returns everything kept. */
    public static synchronized List<Line> after(long seq) {
        List<Line> out = new ArrayList<>();
        for (Line line : LINES) if (line.seq() > seq) out.add(line);
        return out;
    }
}
