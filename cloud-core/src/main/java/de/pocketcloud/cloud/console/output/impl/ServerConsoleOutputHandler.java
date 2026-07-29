package de.pocketcloud.cloud.console.output.impl;

import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.output.OutputHandler;
import de.pocketcloud.cloud.console.output.util.AuthorizedLoggerBase;
import org.jetbrains.annotations.NotNull;

public final class ServerConsoleOutputHandler extends AuthorizedLoggerBase implements OutputHandler {

    @Override
    public boolean canOutput(@NotNull ILogger logger) {
        return isAuthorized(logger);
    }

    @Override
    public void handleOutput(String message) {
        PocketCloud.instance().console().print(message);
    }
}