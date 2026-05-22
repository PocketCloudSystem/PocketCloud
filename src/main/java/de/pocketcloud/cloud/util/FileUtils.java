package de.pocketcloud.cloud.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

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

    public static void createDirs(String... path) {
        for (String s : path) createDir(s);
    }

    public static boolean createDir(String path) {
        try {
            Files.createDirectories(Path.of(path));
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory: " + path, e);
        }
    }

    public static boolean filePutContents(String filePath, String content) {
        try {
            Path path = Path.of(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

    public static String fileGetContents(String filePath) {
        return fileGetContents(filePath, "");
    }

    public static String fileGetContents(String filePath, String defaultValue) {
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) return defaultValue;
            return Files.readString(path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    public static boolean rename(String from, String to) {
        try {
            Files.move(Path.of(from), Path.of(to), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to rename " + from + " to " + to, e);
        }
    }

    public static boolean unlinkFile(String filePath) {
        try {
            return Files.deleteIfExists(Path.of(filePath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + filePath, e);
        }
    }

    public static boolean copyDirectory(String src, String dst, Set<String> exclusions) {
        try {
            Path source = Path.of(src);
            Path target = Path.of(dst);

            if (!Files.isDirectory(source)) {
                throw new IllegalArgumentException("Source directory does not exist: " + src);
            }

            Files.walk(source).forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    String name = relative.toString();

                    for (String ex : exclusions) {
                        if (name.startsWith(ex) || path.getFileName().toString().equals(ex)) {
                            return;
                        }
                    }

                    Path out = target.resolve(relative);

                    if (Files.isDirectory(path)) {
                        Files.createDirectories(out);
                    } else {
                        Files.createDirectories(out.getParent());
                        Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed copying: " + path, e);
                }
            });

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy directory: " + src + " -> " + dst, e);
        }
    }

    public static boolean removeDirectory(String dir) {
        try {
            Path root = Path.of(dir);

            if (!Files.exists(root)) return false;

            Files.walk(root)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed deleting: " + path, e);
                        }
                    });

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove directory: " + dir, e);
        }
    }

    public static String encodeJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static boolean encodeJsonFile(String filePath, Object obj) {
        return filePutContents(filePath, encodeJson(obj));
    }

    public static <T> T decodeJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> T decodeJsonFile(String filePath, Class<T> clazz) {
        return decodeJson(fileGetContents(filePath), clazz);
    }

    public static String emitYaml(Object data) {
        return YAML.dump(data);
    }

    public static boolean emitYamlFile(String filePath, Object data) {
        return filePutContents(filePath, emitYaml(data));
    }

    public static Map<String, Object> parseYaml(String yaml) {
        return YAML.load(yaml);
    }

    public static Map<String, Object> parseYamlFile(String filePath) {
        return parseYaml(fileGetContents(filePath));
    }
}