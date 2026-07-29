package de.pocketcloud.shared.sync;

import de.pocketcloud.common.serialization.Writable;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

public record SyncType(String type) implements Writable<String> {

    private static final ConcurrentHashMap<String, SyncType> customTypes = new ConcurrentHashMap<>();

    public static final SyncType SERVER = new SyncType("SERVER");
    public static final SyncType SERVER_STATUS = new SyncType("SERVER_STATUS");
    public static final SyncType SERVER_STORAGE = new SyncType("SERVER_STORAGE");
    public static final SyncType SERVERS = new SyncType("SERVERS");
    public static final SyncType TEMPLATE = new SyncType("TEMPLATE");
    public static final SyncType TEMPLATES = new SyncType("TEMPLATES");
    public static final SyncType SERVER_GROUP = new SyncType("SERVER_GROUP");
    public static final SyncType SERVER_GROUPS = new SyncType("SERVER_GROUPS");
    public static final SyncType PLAYER = new SyncType("PLAYER");
    public static final SyncType PLAYER_NOTIFICATION_STATE = new SyncType("PLAYER_NOTIFICATION_STATE");
    public static final SyncType PLAYER_WHITELIST_STATE = new SyncType("PLAYER_WHITELIST_STATE");
    public static final SyncType PLAYERS = new SyncType("PLAYERS");
    public static final SyncType LANGUAGE = new SyncType("LANGUAGE");
    public static final SyncType LIBRARIES = new SyncType("LIBRARIES");
    public static final SyncType WHITELIST = new SyncType("WHITELIST");
    public static final SyncType NOTIFICATION_LIST = new SyncType("NOTIFICATION_LIST");
    public static final SyncType SOFTWARES = new SyncType("SOFTWARES");

    static {
        register(SERVER);
        register(SERVER_STATUS);
        register(SERVER_STORAGE);
        register(SERVERS);
        register(TEMPLATE);
        register(TEMPLATES);
        register(SERVER_GROUP);
        register(SERVER_GROUPS);
        register(PLAYER);
        register(PLAYER_NOTIFICATION_STATE);
        register(PLAYER_WHITELIST_STATE);
        register(PLAYERS);
        register(LANGUAGE);
        register(LIBRARIES);
        register(WHITELIST);
        register(NOTIFICATION_LIST);
        register(SOFTWARES);
    }

    @Override
    public String write() {
        return type;
    }

    public static SyncType register(SyncType type) {
        if (customTypes.containsKey(type.type())) throw new IllegalArgumentException("Custom SyncType with name " + type.type() + " already exists");
        customTypes.put(type.type(), type);
        return type;
    }

    public static SyncType register(String name) {
        if (customTypes.containsKey(name)) throw new IllegalArgumentException("Custom SyncType with name " + name + " already exists");
        customTypes.put(name, new SyncType(name));
        return customTypes.get(name);
    }

    @NotNull
    public static SyncType get(@NotNull String type) {
        if (!customTypes.containsKey(type)) return register(type);
        return customTypes.get(type);
    }
}