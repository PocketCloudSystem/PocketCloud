package de.pocketcloud.cloud.server.util;

import de.pocketcloud.cloud.server.start.ProcessServerStartMethod;
import de.pocketcloud.cloud.server.start.ScreenServerStartMethod;
import de.pocketcloud.cloud.server.start.ServerStartMethod;
import de.pocketcloud.cloud.server.start.TmuxServerStartMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ServerStartMethods {

    private static final Map<String, ServerStartMethod> serverStartMethods = new HashMap<>();
    private static ServerStartMethod current = null;

    static {
        serverStartMethods.put("tmux", new TmuxServerStartMethod());
        serverStartMethods.put("screen", new ScreenServerStartMethod());
        serverStartMethods.put("proc", new ProcessServerStartMethod());
    }

    public static void set(ServerStartMethod current) {
        ServerStartMethods.current = current;
    }

    public static Optional<ServerStartMethod> get(String name) {
        return Optional.ofNullable(serverStartMethods.getOrDefault(name, null));
    }

    public static ServerStartMethod current() {
        if (current == null) throw new IllegalStateException("Current ServerStartMethod has not yet been initialized");
        return current;
    }

    public static Map<String, ServerStartMethod> getAll() {
        return serverStartMethods;
    }
}