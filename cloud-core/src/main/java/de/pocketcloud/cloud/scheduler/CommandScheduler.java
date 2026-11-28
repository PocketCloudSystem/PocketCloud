package de.pocketcloud.cloud.scheduler;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.data.ClusterDataModule;
import de.pocketcloud.cloud.console.command.sender.ConsoleCommandSender;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Console commands that run on a cron time ({@code schedule} command), stored in {@code storage/internal/schedules.json}.
 * In a cluster every node has a copy (changes go through the head, see {@link ClusterDataModule}), but only the head
 * runs them, so each command runs once for the whole cluster. Times are in the node's local time zone.
 */
public final class CommandScheduler {

    public record Schedule(String id, String cron, String command) {
    }

    private static final Object LOCK = new Object();
    private static List<Schedule> schedules;
    private static ScheduledExecutorService executor;
    private static LocalDateTime lastMinute;

    private CommandScheduler() {
    }

    private static Path file() {
        return PocketCloudPaths.storage().internal().with("schedules.json").asPath();
    }

    public static List<Schedule> all() {
        synchronized (LOCK) {
            if (schedules == null) schedules = load();
            return List.copyOf(schedules);
        }
    }

    /** Validates and adds a schedule; in a cluster on a node that is not head it is sent to the head. */
    public static Schedule add(String cron, String command) {
        CronExpression.parse(cron);
        if (command.isBlank()) throw new IllegalArgumentException("the command is empty");
        Schedule schedule = new Schedule(HexFormat.of().toHexDigits(ThreadLocalRandom.current().nextInt()).substring(0, 6), cron.trim(), command.trim());
        if (!ClusterDataModule.forwardScheduleAdd(schedule)) addLocal(schedule);
        return schedule;
    }

    /** @return false if there is no schedule with this id (checked against this node's copy) */
    public static boolean remove(String id) {
        if (all().stream().noneMatch(s -> s.id().equals(id))) return false;
        if (!ClusterDataModule.forwardScheduleRemove(id)) removeLocal(id);
        return true;
    }

    /** Head or single node: applies the change and tells the other nodes. */
    public static void addLocal(Schedule schedule) {
        CronExpression.parse(schedule.cron());
        List<Schedule> list = new ArrayList<>(all());
        list.add(schedule);
        replaceAll(list);
        ClusterDataModule.announceSchedules(list);
    }

    public static boolean removeLocal(String id) {
        List<Schedule> list = new ArrayList<>(all());
        if (!list.removeIf(s -> s.id().equals(id))) return false;
        replaceAll(list);
        ClusterDataModule.announceSchedules(list);
        return true;
    }

    /** Replaces this node's copy (also used for the head's announcements and snapshots). */
    public static void replaceAll(List<Schedule> list) {
        synchronized (LOCK) {
            schedules = new ArrayList<>(list);
            try {
                Path file = file();
                Files.createDirectories(file.getParent());
                Path tmp = file.resolveSibling("schedules.json.tmp");
                Files.writeString(tmp, FileUtils.PRETTY_GSON.toJson(schedules));
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                CloudLogger.get().warn("Could not save the scheduled commands§8: §c{}", e.getMessage());
            }
        }
    }

    private static List<Schedule> load() {
        Path file = file();
        if (!Files.isRegularFile(file)) return new ArrayList<>();
        try {
            List<Schedule> list = FileUtils.GSON.fromJson(Files.readString(file), new TypeToken<List<Schedule>>() {}.getType());
            return list == null ? new ArrayList<>() : new ArrayList<>(list);
        } catch (Exception e) {
            CloudLogger.get().warn("Could not read §b{}§8: §c{}", file, e.getMessage());
            return new ArrayList<>();
        }
    }

    // ------------------------------------------------------------------ running

    public static void start() {
        if (executor != null) return;
        lastMinute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES); // not again in the minute the cloud started
        executor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("command-scheduler").factory());
        executor.scheduleAtFixedRate(CommandScheduler::tick, 5, 5, TimeUnit.SECONDS);
    }

    public static void stop() {
        if (executor != null) executor.shutdownNow();
        executor = null;
    }

    private static void tick() {
        try {
            LocalDateTime minute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            if (!minute.isAfter(lastMinute)) return;
            lastMinute = minute;
            ClusterManager cluster = PocketCloud.instance().cluster();
            if (cluster.clustered() && !cluster.isHead()) return;
            for (Schedule schedule : all()) {
                try {
                    if (!CronExpression.parse(schedule.cron()).matches(minute)) continue;
                    CloudLogger.get().info("Running the scheduled command §b{}§8: §r{}", schedule.id(), schedule.command());
                    PocketCloud.instance().commands().execute(new ConsoleCommandSender(), schedule.command());
                } catch (Exception e) {
                    CloudLogger.get().warn("The scheduled command §b{} §rfailed§8: §c{}", schedule.id(), e.getMessage());
                }
            }
        } catch (Exception e) {
            CloudLogger.get().warn("Could not run the scheduled commands§8: §c{}", e.getMessage());
        }
    }
}
