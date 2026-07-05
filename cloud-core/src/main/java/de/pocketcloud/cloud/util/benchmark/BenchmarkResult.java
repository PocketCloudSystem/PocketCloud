package de.pocketcloud.cloud.util.benchmark;

public record BenchmarkResult(String name, int iterations, double avg, double min, double max) {

    public String format(int precision) {
        String fmt = "%." + precision + "fms";
        return String.format(
                "Name: %s | Count: %d | Avg: %s | Min: %s | Max: %s",
                name != null ? name : "N/A",
                iterations,
                String.format(fmt, avg),
                String.format(fmt, min),
                String.format(fmt, max)
        );
    }

    public String format() {
        return format(3);
    }
}