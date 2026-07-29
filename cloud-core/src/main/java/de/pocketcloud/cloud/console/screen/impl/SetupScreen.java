package de.pocketcloud.cloud.console.screen.impl;

import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.screen.Screen;
import de.pocketcloud.cloud.console.util.InterruptionResult;
import de.pocketcloud.cloud.setup.Setup;

public final class SetupScreen extends Screen {

    private final Setup setup;

    public SetupScreen(Setup setup) {
        this.setup = setup;
    }

    @Override
    public void initialize(CloudConsole console) {
        clear();
        console.disableHistory();
        enableCompletion();
    }

    @Override
    public void handleInput(String input) {
        setup.handleInput(input);
    }

    @Override
    public void tick(long currentTick) {}

    @Override
    public void onRemove(long currentTick) {
        clear();
        restoreAll();
        printLogCache();
    }

    @Override
    public InterruptionResult onCancel(long currentTick) {
        setup.cancel();
        return InterruptionResult.CONTINUE;
    }
}