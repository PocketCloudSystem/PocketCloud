package de.pocketcloud.cloud.template.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TemplateHelper {

    public static final List<String> KEYS = List.of(
            "name", "lobby", "maintenance", "staticServers", "alwaysCopyToStaticServers", "saveOnShutdown",
            "maxPlayerCount", "minServerCount", "maxServerCount", "startNewPercentage",
            "autoStart", "templateType", "serverSoftware", "maxMemory"
    );

    public static final List<String> EDITABLE_KEYS = List.of(
            "lobby", "maintenance", "staticServers", "alwaysCopyToStaticServers", "saveOnShutdown",
            "maxPlayerCount", "minServerCount", "maxServerCount",
            "startNewPercentage", "autoStart", "maxMemory"
    );

    public static final Map<String, Class<?>> KEY_TYPES = new HashMap<>();

    private static final Map<String, String> CONVERSION = Map.of(
            "staticservers", "staticServers",
            "alwayscopytostaticservers", "alwaysCopyToStaticServers",
            "maxplayercount", "maxPlayerCount",
            "minservercount", "minServerCount",
            "maxservercount", "maxServerCount",
            "startnewpercentage", "startNewPercentage",
            "autostart", "autoStart",
            "templatetype", "templateType",
            "serversoftware", "serverSoftware"
    );

    static {
        KEY_TYPES.put("lobby", Boolean.class);
        KEY_TYPES.put("maintenance", Boolean.class);
        KEY_TYPES.put("staticServers", Boolean.class);
        KEY_TYPES.put("alwaysCopyToStaticServers", Boolean.class);
        KEY_TYPES.put("saveOnShutdown", Boolean.class);
        KEY_TYPES.put("maxPlayerCount", Integer.class);
        KEY_TYPES.put("minServerCount", Integer.class);
        KEY_TYPES.put("maxServerCount", Integer.class);
        KEY_TYPES.put("startNewPercentage", Double.class);
        KEY_TYPES.put("autoStart", Boolean.class);
        KEY_TYPES.put("templateType", String.class);
        KEY_TYPES.put("serverSoftware", String.class);
        KEY_TYPES.put("maxMemory", Integer.class);
    }

    private static final Set<String> EDITABLE_KEYS_SET = Set.copyOf(EDITABLE_KEYS);

    public static String convert(String key) {
        return CONVERSION.getOrDefault(key.toLowerCase(), key);
    }

    public static boolean checkKey(String key) {
        return EDITABLE_KEYS_SET.contains(key);
    }

    public static Class<?> getKeyType(String key) {
        return KEY_TYPES.get(key);
    }
}