package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.common.lifecycle.Tickable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PacketQueue implements Tickable {

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public void submit(Runnable task) {
        tasks.add(task);
    }

    @Override
    public void tick(long currentTick) {
        long deadline = System.nanoTime() + 20_000_000L;
        Runnable task;
        while (System.nanoTime() < deadline && (task = tasks.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                CloudLogger.get().exception(e);
            }
        }
    }
}