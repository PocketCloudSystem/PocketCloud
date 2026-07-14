package de.pocketcloud.cloud.console.screen.impl;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.command.sender.ConsoleCommandSender;
import de.pocketcloud.cloud.console.screen.Screen;

public final class DefaultScreen extends Screen {

    @Override
    public void initialize(CloudConsole console) {
        console.resetPrompt();
        console.enableCompletion();
    }

    @Override
    public void tick(long currentTick) {}

    @Override
    public void onRemove(long currentTick) {}

    @Override
    public void onCancel(long currentTick) {
        PocketCloud.instance().commands().call(new ConsoleCommandSender(), "exit -y");
    }

    @Override
    public void handleInput(String input) {
        PocketCloud.instance().commands().call(new ConsoleCommandSender(), input);
    }
}