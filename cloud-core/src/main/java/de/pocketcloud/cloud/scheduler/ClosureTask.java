package de.pocketcloud.cloud.scheduler;

import java.util.function.Consumer;

public final class ClosureTask extends Task {

    private final Consumer<Long> onRun;
    private final Consumer<Long> onCancel;

    public ClosureTask(Consumer<Long> onRun, Consumer<Long> onCancel) {
        this.onRun = onRun;
        this.onCancel = onCancel;
    }

    @Override
    public void onRun(long currentTick) {
        onRun.accept(currentTick);
    }

    @Override
    public void onCancel(long currentTick) {
        if (onCancel != null) {
            onCancel.accept(currentTick);
        }
    }
}