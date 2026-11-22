package de.pocketcloud.cloud.cluster.recovery;

import de.pocketcloud.cloud.server.CloudServer;

import java.util.HashMap;
import java.util.Map;

/**
 * A server process this node started, as written to {@code storage/internal/cluster/local-servers.json}.
 */
public final class LocalServerRecord {

    String name;
    String uuid;
    String template;
    int id;
    int port;
    Long pid;
    long startTime;
    String status;

    LocalServerRecord() {
    }

    static LocalServerRecord of(CloudServer server) {
        LocalServerRecord record = new LocalServerRecord();
        record.name = server.name();
        record.uuid = server.uuid().toString();
        record.template = server.templateName();
        record.id = server.id();
        record.port = server.data().port();
        record.pid = server.data().processId();
        record.startTime = server.startTime() == null ? 0 : server.startTime().toEpochMilli();
        record.status = server.status().name();
        return record;
    }

    Map<String, Object> write() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("uuid", uuid);
        map.put("template", template);
        map.put("id", id);
        map.put("port", port);
        map.put("pid", pid == null ? -1L : pid);
        map.put("status", status);
        return map;
    }

    static LocalServerRecord read(Map<String, Object> map) {
        LocalServerRecord record = new LocalServerRecord();
        record.name = String.valueOf(map.get("name"));
        record.uuid = String.valueOf(map.get("uuid"));
        record.template = String.valueOf(map.get("template"));
        record.id = map.get("id") instanceof Number n ? n.intValue() : 0;
        record.port = map.get("port") instanceof Number n ? n.intValue() : 0;
        long pid = map.get("pid") instanceof Number n ? n.longValue() : -1L;
        record.pid = pid > 0 ? pid : null;
        record.status = String.valueOf(map.get("status"));
        return record;
    }

    /**
     * Whether the process still runs. On Linux also checks that its working directory belongs to the
     * server, so a recycled pid (e.g. after a reboot) is never adopted or killed.
     */
    boolean alive() {
        if (pid == null || !ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) return false;
        java.nio.file.Path cwd = java.nio.file.Path.of("/proc", String.valueOf(pid), "cwd");
        try {
            String dir = java.nio.file.Files.readSymbolicLink(cwd).toString();
            return dir.contains(uuid) || dir.endsWith("/" + name);
        } catch (Exception e) {
            return true; // no procfs or no permission: trust the pid
        }
    }

    @Override
    public String toString() {
        return name + " (pid " + pid + ", port " + port + ")";
    }
}
