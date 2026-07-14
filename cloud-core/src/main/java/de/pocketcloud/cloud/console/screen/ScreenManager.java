package de.pocketcloud.cloud.console.screen;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.screen.impl.DefaultScreen;
import de.pocketcloud.common.lifecycle.Tickable;
import lombok.Getter;
import lombok.experimental.Accessors;

public final class ScreenManager implements Tickable {

    private Screen currentScreen = null;

    @Override
    public void tick(long currentTick) {
        if (currentScreen == null) return;
        this.currentScreen.tick(currentTick);
    }

    public void set(Screen screen) {
        if (currentScreen != null) currentScreen.onRemove(PocketCloud.instance().currentTick());
        this.currentScreen = screen;
        screen.initialize(PocketCloud.instance().console());
    }

    public void reset() {
        currentScreen = new DefaultScreen();
    }

    public Screen get() {
        return currentScreen;
    }
}