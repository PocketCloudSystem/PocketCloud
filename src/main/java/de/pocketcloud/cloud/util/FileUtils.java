package de.pocketcloud.cloud.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.yaml.snakeyaml.Yaml;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class FileUtils {

    public static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Gson GSON = new Gson();
    public static final Yaml YAML = new Yaml();

    public static String extensionOf(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf(".");
        String extension = null;
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) extension = fileName.substring(dotIndex + 1);
        return extension;
    }

    public static void createDirs(Path... path) {
        for (Path s : path) createDir(s);
    }

    public static boolean createDir(Path path) {
        try {
            Files.createDirectories(path);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory: " + path, e);
        }
    }

    public static boolean filePutContents(Path filePath, String content) {
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

    public static String fileGetContents(Path filePath) {
        return fileGetContents(filePath, "");
    }

    public static String fileGetContents(Path filePath, String defaultValue) {
        try {
            if (!Files.exists(filePath)) return defaultValue;
            return Files.readString(filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    public static boolean rename(Path from, Path to) {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to rename " + from + " to " + to, e);
        }
    }

    public static boolean unlinkFile(Path filePath) {
        try {
            return Files.deleteIfExists(filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + filePath, e);
        }
    }

    public static boolean copyDirectory(Path source, Path destination, Set<String> exclusions) {
        try {
            if (!Files.isDirectory(source)) {
                throw new IllegalArgumentException("Source directory does not exist: " + source);
            }

            try (Stream<Path> paths = Files.walk(source)) {
                paths.forEach(path -> {
                    try {
                        Path relative = source.relativize(path);
                        String name = relative.toString();

                        for (String ex : exclusions) {
                            if (name.startsWith(ex) || path.getFileName().toString().equals(ex)) {
                                return;
                            }
                        }

                        if (Files.isDirectory(path)) {
                            Files.createDirectories(destination);
                        } else {
                            Files.createDirectories(destination.getParent());
                            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed copying: " + path, e);
                    }
                });
            }

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy directory: " + source + " -> " + destination, e);
        }
    }

    public static boolean removeDirectory(Path dir) {
        try {
            if (!Files.exists(dir)) return false;

            try (Stream<Path> paths = Files.walk(dir).sorted(Comparator.reverseOrder())) {
                paths.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed deleting: " + path, e);
                    }
                });
            }

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove directory: " + dir, e);
        }
    }

    public static String encodeJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static boolean encodeJsonFile(Path filePath, Object obj) {
        return filePutContents(filePath, encodeJson(obj));
    }

    public static <T> T decodeJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> T decodeJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }

    public static <T> T decodeJson(String json, TypeToken<T> typeToken) {
        return GSON.fromJson(json, typeToken);
    }

    public static <T> T decodeJsonFile(Path filePath, Class<T> clazz) {
        return decodeJson(fileGetContents(filePath), clazz);
    }

    public static <T> T decodeJsonFile(Path filePath, Type type) {
        return decodeJson(fileGetContents(filePath), type);
    }

    public static <T> T decodeJsonFile(Path filePath, TypeToken<T> typeToken) {
        return decodeJson(fileGetContents(filePath), typeToken);
    }

    public static String emitYaml(Object data) {
        return YAML.dump(data);
    }

    public static boolean emitYamlFile(Path filePath, Object data) {
        return filePutContents(filePath, emitYaml(data));
    }

    public static Map<String, Object> parseYaml(String yaml) {
        return YAML.load(yaml);
    }

    public static Map<String, Object> parseYamlFile(Path filePath) {
        return parseYaml(fileGetContents(filePath));
    }
}