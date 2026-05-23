package de.pocketcloud.cloud.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class PocketCloudPaths {

    public static final String STORAGE = "storage";

    public static final String STORAGE_CONFIGS = "storage/configs";
    public static final String STORAGE_TIMINGS = "storage/timings";
    public static final String STORAGE_BACKUPS = "storage/backups";
    public static final String STORAGE_INTERNAL = "storage/internal";
    public static final String STORAGE_INGAME = "storage/inGame";

    public static final String STORAGE_CRASHES = "storage/crashes";
    public static final String STORAGE_CRASHES_SERVER = "storage/crashes/server";
    public static final String STORAGE_CRASHES_CLOUD = "storage/crashes/cloud";

    public static final String STORAGE_LIBRARIES = "storage/libraries";
    public static final String STORAGE_PLUGINS = "storage/plugins";
    public static final String STORAGE_SOFTWARE = "storage/software";
    public static final String STORAGE_STATIC_SERVERS = "storage/staticServers";

    public static final String TMP = "tmp";

    public static final String TEMPLATES = "templates";
    public static final String TEMPLATES_GLOBAL = "templates/global";

    public static final String GROUPS = "groups";

    public static final List<String> ALL_DIRECTORIES = List.of(
            STORAGE,
            STORAGE_CONFIGS,
            STORAGE_TIMINGS,
            STORAGE_BACKUPS,
            STORAGE_INTERNAL,
            STORAGE_INGAME,
            STORAGE_CRASHES,
            STORAGE_CRASHES_SERVER,
            STORAGE_CRASHES_CLOUD,
            STORAGE_LIBRARIES,
            STORAGE_PLUGINS,
            STORAGE_SOFTWARE,
            STORAGE_STATIC_SERVERS,
            TMP,
            TEMPLATES,
            TEMPLATES_GLOBAL,
            GROUPS
    );

    public static Node storage() {
        return new Node("storage");
    }

    public static Node tmp() {
        return new Node("tmp");
    }

    public static Node templates() {
        return new Node("templates");
    }

    public static Node groups() {
        return new Node("groups");
    }

    public static final class Node {

        private final List<String> parts = new ArrayList<>();

        private Node(String root) {
            parts.add(root);
        }

        private Node(List<String> parent, String next) {
            parts.addAll(parent);
            parts.add(next);
        }

        public Node configs() {
            return with("configs");
        }

        public Node timings() {
            return with("timings");
        }

        public Node backups() {
            return with("backups");
        }

        public Node internal() {
            return with("internal");
        }

        public Node inGame() {
            return with("inGame");
        }

        public Node staticServers() {
            return with("staticServers");
        }

        public Node crashes() {
            return with("crashes");
        }

        public Node server() {
            return with("server");
        }

        public Node cloud() {
            return with("cloud");
        }

        public Node libraries() {
            return with("libraries");
        }

        public Node plugins() {
            return with("plugins");
        }

        public Node software() {
            return with("software");
        }

        public Node global() {
            return with("global");
        }

        public Node with(String name) {
            return new Node(parts, name);
        }

        public String asString() {
            try {
                return asPath().toAbsolutePath().toString();
            } catch (Exception _) {
                return asPath().toString();
            }
        }

        public Path asPath() {
            return Paths.get(parts.getFirst(), parts.subList(1, parts.size()).toArray(new String[0]));
        }

        @Override
        public String toString() {
            return asString();
        }
    }
}