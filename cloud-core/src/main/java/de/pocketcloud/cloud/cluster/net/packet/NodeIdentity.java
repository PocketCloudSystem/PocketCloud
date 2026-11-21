package de.pocketcloud.cloud.cluster.net.packet;

import java.util.Map;

/**
 * Static facts about a node, exchanged in the hello.
 */
public record NodeIdentity(String name, String address, int port, long startedAt, long term, String head, String version) {

    public Map<String, Object> write() {
        return Map.of("name", name, "address", address, "port", port, "startedAt", startedAt,
                "term", term, "head", head == null ? "" : head, "version", version);
    }

    public static NodeIdentity read(Map<String, Object> map) {
        return new NodeIdentity(
                String.valueOf(map.get("name")),
                String.valueOf(map.get("address")),
                ((Number) map.get("port")).intValue(),
                ((Number) map.get("startedAt")).longValue(),
                ((Number) map.get("term")).longValue(),
                String.valueOf(map.getOrDefault("head", "")),
                String.valueOf(map.getOrDefault("version", "")));
    }
}
