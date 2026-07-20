package de.pocketcloud.cloud.console.output.impl;

import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.output.OutputHandler;
import org.jetbrains.annotations.NotNull;

public final class DefaultOutputHandler implements OutputHandler {

    @Override
    public boolean canOutput(@NotNull ILogger logger) {
        return true;
    }

    @Override
    public void handleOutput(String message) {
        PocketCloud.instance().console().print(message);
    }
}