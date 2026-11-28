package de.pocketcloud.cloud.scheduler;

import java.time.LocalDateTime;
import java.util.BitSet;

/**
 * Standard 5-field cron: minute hour day-of-month month day-of-week. Fields take {@code *}, numbers, ranges
 * ({@code 1-5}), lists ({@code 1,15}) and steps ({@code *}{@code /15}, {@code 0-30/10}). Day of week 0-7, 0 and 7 are Sunday.
 * As in cron: if day of month and day of week are both restricted, a day matching either one matches.
 */
public final class CronExpression {

    private static final int[][] RANGES = {{0, 59}, {0, 23}, {1, 31}, {1, 12}, {0, 7}};
    private static final String[] NAMES = {"minute", "hour", "day of month", "month", "day of week"};

    private final BitSet[] fields = new BitSet[5];
    private final boolean anyDayOfMonth;
    private final boolean anyDayOfWeek;

    private CronExpression(String[] parts) {
        for (int i = 0; i < 5; i++) fields[i] = field(parts[i], RANGES[i][0], RANGES[i][1], NAMES[i]);
        if (fields[4].get(7)) fields[4].set(0);
        anyDayOfMonth = parts[2].equals("*");
        anyDayOfWeek = parts[4].equals("*");
    }

    /** @throws IllegalArgumentException with a readable reason */
    public static CronExpression parse(String expression) {
        String[] parts = expression.trim().split("\\s+");
        if (parts.length != 5) throw new IllegalArgumentException("a cron time has 5 fields (minute hour day month weekday), e.g. \"0 4 * * *\"");
        return new CronExpression(parts);
    }

    public boolean matches(LocalDateTime time) {
        if (!fields[0].get(time.getMinute()) || !fields[1].get(time.getHour()) || !fields[3].get(time.getMonthValue())) return false;
        boolean dom = fields[2].get(time.getDayOfMonth());
        boolean dow = fields[4].get(time.getDayOfWeek().getValue() % 7);
        if (anyDayOfMonth || anyDayOfWeek) return dom && dow;
        return dom || dow;
    }

    private static BitSet field(String text, int min, int max, String name) {
        BitSet bits = new BitSet();
        for (String part : text.split(",")) {
            try {
                String range = part;
                int step = 1;
                int slash = part.indexOf('/');
                if (slash >= 0) {
                    range = part.substring(0, slash);
                    step = Integer.parseInt(part.substring(slash + 1));
                    if (step <= 0) throw new NumberFormatException();
                }
                int from, to;
                if (range.equals("*")) {
                    from = min;
                    to = max;
                } else if (range.contains("-")) {
                    String[] ends = range.split("-", 2);
                    from = Integer.parseInt(ends[0]);
                    to = Integer.parseInt(ends[1]);
                } else {
                    from = Integer.parseInt(range);
                    to = slash >= 0 ? max : from;
                }
                if (from < min || to > max || from > to) throw new IllegalArgumentException("the " + name + " must be between " + min + " and " + max + ": " + part);
                for (int v = from; v <= to; v += step) bits.set(v);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid " + name + ": " + part);
            }
        }
        return bits;
    }
}
