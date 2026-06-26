package de.pocketcloud.cloud.scheduler;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.plugin.CloudPlugin;
import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

@Getter
public final class TaskHandler {

    private final long id;
    private final long nextRun;

    private final Task task;
    private final int interval;
    private final boolean repeating;
    private final CloudPlugin plugin;

    private boolean cancelled = false;

    public TaskHandler(Task task, int delay, int interval, boolean repeating, CloudPlugin plugin) {
        this.task = task;
        this.interval = interval;
        this.repeating = repeating;
        this.plugin = plugin;

        this.id = ThreadLocalRandom.current().nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
        this.nextRun = PocketCloud.instance().currentTick() + delay;
    }

    public void cancel() {
        if (cancelled) return;
        cancelled = true;
        try {
            task.onCancel(PocketCloud.instance().currentTick());
        } catch (Exception e) {
            plugin.getLogger().exception("Uncaught exception occurred during onCancel of task {} on tick {}", e, task.getClass().getName(), PocketCloud.instance().currentTick());
        }
    }

    public void update(long currentTick) {
        if (currentTick >= nextRun) {
            try {
                task.onRun(currentTick);
            } catch (Exception e) {
                plugin.getLogger().exception("Uncaught exception occurred during the run of task {} on tick {}", e, task.getClass().getName(), currentTick);
                this.cancel();
            } finally {
                if (!this.repeating) cancel();
            }
        }
    }
}