package de.pocketcloud.cloud.console.output;

import de.pocketcloud.cloud.console.log.ILogger;
import org.jetbrains.annotations.NotNull;

public interface OutputHandler {

    boolean canOutput(@NotNull ILogger logger);

    void handleOutput(String message);
}