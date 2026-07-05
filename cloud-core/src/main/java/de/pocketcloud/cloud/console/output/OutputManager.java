package de.pocketcloud.cloud.console.output;

import de.pocketcloud.cloud.console.output.impl.DefaultOutputHandler;

public final class OutputManager {

    private static OutputHandler outputHandler = null;

    public static void set(OutputHandler outputHandler) {
        OutputManager.outputHandler = outputHandler;
    }

    public static void reset() {
        OutputManager.outputHandler = new DefaultOutputHandler();
    }

    public static OutputHandler get() {
        if (outputHandler == null) reset();
        return outputHandler;
    }
}