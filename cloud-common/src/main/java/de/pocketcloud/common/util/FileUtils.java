package de.pocketcloud.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public final class FileUtils {

    public static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "I/O-Worker");
                t.setDaemon(true);
                return t;
            }
    );

    public static final Gson PRETTY_GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).setPrettyPrinting().create();
    public static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    public static final ObjectMapper YAML = new ObjectMapper(
            YAMLFactory.builder()
                    .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                    .disable(YAMLWriteFeature.USE_NATIVE_TYPE_ID)
                    .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                    .enable(YAMLWriteFeature.INDENT_ARRAYS)
                    .build()
    );

    public static String extensionOf(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex > 0 && dotIndex < fileName.length() - 1) ? fileName.substring(dotIndex + 1) : null;
    }

    public static void createDirs(Path... paths) {
        for (Path path : paths) createDir(path);
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
            if (!Files.isDirectory(source)) throw new IllegalArgumentException("Source is not a directory: " + source);
            try (Stream<Path> paths = Files.walk(source)) {
                paths.forEach(path -> {
                    try {
                        Path relative = source.relativize(path);
                        String name = relative.toString();
                        for (String ex : exclusions) {
                            if (name.startsWith(ex) || path.getFileName().toString().equals(ex)) return;
                        }
                        if (Files.isDirectory(path)) {
                            Files.createDirectories(destination.resolve(relative));
                        } else {
                            Files.createDirectories(destination.resolve(relative).getParent());
                            Files.copy(path, destination.resolve(relative), StandardCopyOption.REPLACE_EXISTING);
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
        try {
            return YAML.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize YAML", e);
        }
    }

    public static boolean emitYamlFile(Path filePath, Object data) {
        return filePutContents(filePath, emitYaml(data));
    }

    public static <T> T parseYaml(String yaml, Class<T> clazz) {
        try {
            return YAML.readValue(yaml, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YAML", e);
        }
    }

    public static <T> T parseYaml(String yaml, TypeReference<T> typeRef) {
        try {
            return YAML.readValue(yaml, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YAML", e);
        }
    }

    public static Map<String, Object> parseYaml(String yaml) {
        return parseYaml(yaml, new TypeReference<>() {});
    }

    public static <T> T parseYamlFile(Path filePath, Class<T> clazz) {
        return parseYaml(fileGetContents(filePath), clazz);
    }

    public static <T> T parseYamlFile(Path filePath, TypeReference<T> typeRef) {
        return parseYaml(fileGetContents(filePath), typeRef);
    }

    public static Map<String, Object> parseYamlFile(Path filePath) {
        return parseYaml(fileGetContents(filePath));
    }

    public static CompletableFuture<Boolean> createDirAsync(Path path) {
        return CompletableFuture.supplyAsync(() -> createDir(path), IO_EXECUTOR);
    }

    public static CompletableFuture<Boolean> filePutContentsAsync(Path filePath, String content) {
        return CompletableFuture.supplyAsync(() -> filePutContents(filePath, content), IO_EXECUTOR);
    }

    public static CompletableFuture<String> fileGetContentsAsync(Path filePath) {
        return CompletableFuture.supplyAsync(() -> fileGetContents(filePath), IO_EXECUTOR);
    }

    public static CompletableFuture<String> fileGetContentsAsync(Path filePath, String defaultValue) {
        return CompletableFuture.supplyAsync(() -> fileGetContents(filePath, defaultValue), IO_EXECUTOR);
    }

    public static CompletableFuture<Boolean> renameAsync(Path from, Path to) {
        return CompletableFuture.supplyAsync(() -> rename(from, to), IO_EXECUTOR);
    }

    public static CompletableFuture<Boolean> unlinkFileAsync(Path filePath) {
        return CompletableFuture.supplyAsync(() -> unlinkFile(filePath), IO_EXECUTOR);
    }

    public static CompletableFuture<Boolean> copyDirectoryAsync(Path source, Path destination, Set<String> exclusions) {
        return CompletableFuture.supplyAsync(() -> copyDirectory(source, destination, exclusions), IO_EXECUTOR);
    }

    public static CompletableFuture<Boolean> removeDirectoryAsync(Path dir) {
        return CompletableFuture.supplyAsync(() -> removeDirectory(dir), IO_EXECUTOR);
    }

    public static CompletableFuture<Boolean> encodeJsonFileAsync(Path filePath, Object obj) {
        return CompletableFuture.supplyAsync(() -> encodeJsonFile(filePath, obj), IO_EXECUTOR);
    }

    public static <T> CompletableFuture<T> decodeJsonFileAsync(Path filePath, Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> decodeJsonFile(filePath, clazz), IO_EXECUTOR);
    }

    public static <T> CompletableFuture<T> decodeJsonFileAsync(Path filePath, Type type) {
        return CompletableFuture.supplyAsync(() -> decodeJsonFile(filePath, type), IO_EXECUTOR);
    }

    public static <T> CompletableFuture<T> decodeJsonFileAsync(Path filePath, TypeToken<T> typeToken) {
        return CompletableFuture.supplyAsync(() -> decodeJsonFile(filePath, typeToken), IO_EXECUTOR);
    }

    public static CompletableFuture<Boolean> emitYamlFileAsync(Path filePath, Object data) {
        return CompletableFuture.supplyAsync(() -> emitYamlFile(filePath, data), IO_EXECUTOR);
    }

    public static CompletableFuture<Map<String, Object>> parseYamlFileAsync(Path filePath) {
        return CompletableFuture.supplyAsync(() -> parseYamlFile(filePath), IO_EXECUTOR);
    }

    public static <T> CompletableFuture<T> parseYamlFileAsync(Path filePath, Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> parseYamlFile(filePath, clazz), IO_EXECUTOR);
    }

    public static <T> CompletableFuture<T> parseYamlFileAsync(Path filePath, TypeReference<T> typeRef) {
        return CompletableFuture.supplyAsync(() -> parseYamlFile(filePath, typeRef), IO_EXECUTOR);
    }
}