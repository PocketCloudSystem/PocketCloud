package de.pocketcloud.cloud.util.benchmark;

import de.pocketcloud.cloud.PocketCloud;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Benchmark {

    private static final Map<String, BenchmarkTiming> timings = new ConcurrentHashMap<>();
    private static final Map<String, TimingSummaryAccumulator> timingsSummary = new ConcurrentHashMap<>();

    private Benchmark() {}

    public static BenchmarkResult measure(Runnable fn, int iterations, String name) {
        double[] times = new double[iterations];
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            fn.run();
            long end = System.nanoTime();
            times[i] = (end - start) / 1_000_000.0;
        }

        double sum = 0, min = Double.MAX_VALUE, max = 0;
        for (double t : times) {
            sum += t;
            if (t < min) min = t;
            if (t > max) max = t;
        }

        return new BenchmarkResult(name, iterations, sum / iterations, min, max);
    }

    public static BenchmarkResult measure(Runnable fn, int iterations) {
        return measure(fn, iterations, null);
    }

    public static BenchmarkResult measure(Runnable fn) {
        return measure(fn, 1, null);
    }

    public static boolean writeTimings(Path path, boolean override) {
        Path parent = path.getParent();
        if (parent != null && !Files.isDirectory(parent)) return false;
        if (Files.exists(path)) {
            if (!override) return false;
            try {
                Files.delete(path);
            } catch (IOException e) {
                return false;
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
            for (BenchmarkTimingsSummary summary : getSummary(null, null)) {
                writer.write(summary.format());
                writer.newLine();
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static BenchmarkTiming startTiming(String name) {
        BenchmarkTiming timing = new BenchmarkTiming(name, PocketCloud.getInstance().ticker().tickCounter());
        timings.put(name, timing);
        timing.startTiming();
        return timing;
    }

    public static BenchmarkTiming stopTiming(String name) {
        BenchmarkTiming timing = timings.remove(name);
        if (timing == null) throw new RuntimeException("No timings started for '" + name + "'");
        timing.stopTiming();

        timingsSummary.compute(name, (k, acc) -> {
            if (acc == null) acc = new TimingSummaryAccumulator();
            acc.count++;
            acc.sum += timing.duration();
            acc.min = Math.min(acc.min, timing.duration());
            acc.max = Math.max(acc.max, timing.duration());
            acc.lastTick = timing.currentTick();
            return acc;
        });

        return timing;
    }

    public static List<BenchmarkTimingsSummary> getSummary(String name, Comparator<BenchmarkTimingsSummary> sortFn) {
        List<String> keys = name != null ? List.of(name) : new ArrayList<>(timingsSummary.keySet());
        List<BenchmarkTimingsSummary> result = new ArrayList<>();

        for (String key : keys) {
            TimingSummaryAccumulator acc = timingsSummary.get(key);
            if (acc == null) continue;
            result.add(new BenchmarkTimingsSummary(key, acc.count, acc.sum / acc.count, acc.min, acc.max, acc.lastTick));
        }

        if (sortFn != null && name == null) result.sort(sortFn);
        return result;
    }

    public static BenchmarkTimingsSummary getSummary(String name) {
        return getSummary(name, null).stream().findFirst().orElse(null);
    }

    public static List<BenchmarkTimingsSummary> getSummary(Comparator<BenchmarkTimingsSummary> sortFn) {
        return getSummary(null, sortFn);
    }

    public static void reset() {
        timings.clear();
        timingsSummary.clear();
    }

    private static final class TimingSummaryAccumulator {

        long count = 0;
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = 0;
        long lastTick = 0;
    }
}