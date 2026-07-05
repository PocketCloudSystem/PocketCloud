package de.pocketcloud.common.util;

public final class ProcessUtils {

    public static boolean kill(long pid, boolean force) {
        return ProcessHandle.of(pid)
                .map(ph -> force ? ph.destroyForcibly() : ph.destroy())
                .orElse(false);
    }

    public static boolean kill(long pid) {
        return kill(pid, false);
    }
}