package de.pocketcloud.cloud.scheduler;

import lombok.Getter;
import lombok.Setter;

public abstract class Task {

    @Setter
    @Getter
    private TaskHandler handler = null;

    abstract public void onRun(long currentTick);

    public void onCancel(long currentTick) {}
}