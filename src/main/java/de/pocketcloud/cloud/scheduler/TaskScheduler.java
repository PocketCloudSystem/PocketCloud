package de.pocketcloud.cloud.scheduler;

import de.pocketcloud.cloud.plugin.CloudPlugin;
import de.pocketcloud.cloud.tick.Tickable;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class TaskScheduler implements Tickable {

    private final Map<Long, TaskHandler> tasks = new HashMap<>();
    @Getter
    private final CloudPlugin owner;

    public TaskScheduler(CloudPlugin owner) {
        this.owner = owner;
    }

    @Override
    public void tick(long currentTick) {
        for (Map.Entry<Long, TaskHandler> entry : tasks.entrySet()) {
            if (entry.getValue().isCancelled()) {
                tasks.remove(entry.getKey());
                continue;
            }

            entry.getValue().update(currentTick);
        }
    }

    public void scheduleDelayedTask(Task task, int delay) {
        scheduleTask(task, delay, 0, false);
    }

    public void scheduleDelayedTask(Consumer<Long> task, int delay) {
        scheduleTask(task, delay, 0, false);
    }

    public void scheduleRepeatingTask(Task task, int interval) {
        scheduleTask(task, 0, interval, true);
    }

    public void scheduleRepeatingTask(Consumer<Long> task, int interval) {
        scheduleTask(task, 0, interval, true);
    }

    public void scheduleDelayedRepeatingTask(Task task, int delay, int interval) {
        scheduleTask(task, delay, interval, true);
    }

    public void scheduleDelayedRepeatingTask(Consumer<Long> task, int delay, int interval) {
        scheduleTask(task, delay, interval, true);
    }

    private void scheduleTask(Task task, int delay, int interval, boolean repeating) {
        TaskHandler taskHandler = new TaskHandler(task, delay, interval, repeating, owner);
        task.setHandler(taskHandler);
        tasks.put(taskHandler.getId(), taskHandler);
    }

    private void scheduleTask(Consumer<Long> task, int delay, int interval, boolean repeating) {
        scheduleTask(task, null, delay, interval, repeating);
    }

    private void scheduleTask(Consumer<Long> task, Consumer<Long> onCancel, int delay, int interval, boolean repeating) {
        scheduleTask(new ClosureTask(task, onCancel),  delay, interval, repeating);
    }

    public void cancel(Task task) {
        if (tasks.containsKey(task.getHandler().getId())) {
            tasks.get(task.getHandler().getId()).cancel();
            tasks.remove(task.getHandler().getId());
        }
    }

    public void cancelAll() {
        for (TaskHandler task : tasks.values()) cancel(task.getTask());
    }

    public Optional<TaskHandler> get(long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public Map<Long, TaskHandler> getAll() {
        return tasks;
    }
}