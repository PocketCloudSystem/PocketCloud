package de.pocketcloud.cloud.util;

import java.util.concurrent.TimeUnit;

public final class TerminalUtils {

    public static boolean isInstalled(String command) {
        try {
            Process process = new ProcessBuilder("bash", "-c", "which " + command)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (finished) {
                int exitCode = process.exitValue();
                return exitCode == 0;
            } else {
                process.destroy();
            }
        } catch (Exception _) {}
        return false;
    }

    public static String shellEscape(String arg) {
        return "'" + arg.replace("'", "'\\''") + "'";
    }
}