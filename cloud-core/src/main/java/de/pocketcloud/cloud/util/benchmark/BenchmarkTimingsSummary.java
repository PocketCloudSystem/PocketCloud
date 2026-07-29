package de.pocketcloud.cloud.util.benchmark;

public record BenchmarkTimingsSummary(String name, long count, double avg, double min, double max, long lastTick) {

    public String format(int precision, boolean colorful) {
        String fmt = "%." + precision + "fms";
        if (colorful) {
            return String.format(
                    "Name: §b%s §8| §rCount: §b%d §8| §rLast: §btick %d §8| §rAvg: §b%s §8| §rMin: §b%s §8| §rMax: §b%s",
                    name,
                    count,
                    lastTick,
                    String.format(fmt, avg),
                    String.format(fmt, min),
                    String.format(fmt, max)
            );
        } else {
            return String.format(
                    "Name: %s | Count: %d | Last: tick %d | Avg: %s | Min: %s | Max: %s",
                    name,
                    count,
                    lastTick,
                    String.format(fmt, avg),
                    String.format(fmt, min),
                    String.format(fmt, max)
            );
        }
    }

    public String format(int precision) {
        return format(precision, false);
    }

    public String format() {
        return format(3, false);
    }
}