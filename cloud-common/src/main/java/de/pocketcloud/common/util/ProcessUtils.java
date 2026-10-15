package de.pocketcloud.common.util;

import java.util.stream.Stream;

public final class ProcessUtils {

    public static boolean kill(long pid, boolean force) {
        return ProcessHandle.of(pid)
                .map(ph -> {
                    Stream<ProcessHandle> all = Stream.concat(ph.descendants(), Stream.of(ph));
                    all.forEach(p -> {
                        if (force) p.destroyForcibly();
                        else p.destroy();
                    });
                    return true;
                })
                .orElse(false);
    }

    public static boolean kill(long pid) {
        return kill(pid, false);
    }
}