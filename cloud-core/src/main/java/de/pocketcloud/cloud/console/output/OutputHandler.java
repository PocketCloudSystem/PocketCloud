package de.pocketcloud.cloud.console.output;

import de.pocketcloud.api.logging.ILogger;
import org.jetbrains.annotations.NotNull;

public interface OutputHandler {

    boolean canOutput(@NotNull ILogger logger);

    void handleOutput(String message);
}