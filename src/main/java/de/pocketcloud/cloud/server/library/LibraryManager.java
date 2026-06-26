package de.pocketcloud.cloud.server.library;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.load.Loadable;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.util.FileUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.file.Files;
import java.util.*;

public final class LibraryManager implements Loadable {

    @Getter
    @Accessors(fluent = true)
    private static LibraryManager instance = null;

    public static final List<Library> DEFAULTS = List.of(
            new Library("forms", "https://github.com/PocketCloudSystem/BetterForms/archive/refs/heads/main.zip", "", "src/", List.of("pmmp-latest"), true)
    );

    private final Map<String, Library> libraries = new LinkedHashMap<>();

    public LibraryManager() {
        instance = this;
    }

    public void load() {
        try {
            if (!Files.isRegularFile(PocketCloudPaths.storage().libraries().with("libraries.json").asPath())) FileUtils.filePutContents(
                    PocketCloudPaths.storage().libraries().with("libraries.json").asPath(),
                    FileUtils.PRETTY_GSON.toJson(DEFAULTS)
            );

            List<Library> libs = FileUtils.decodeJsonFile(PocketCloudPaths.storage().libraries().with("libraries.json").asPath(), new TypeToken<List<Library>>(){});
            for (Library lib : libs) {
                loadLibrary(lib);
            }
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to load bridge libraries", e);
            PocketCloud.instance().shutdown();
        }

        for (Library library : DEFAULTS) {
            if (!libraries.containsKey(library.name())) {
                loadLibrary(library);
            }
        }
    }

    public void loadLibrary(Library library) {
        libraries.put(library.name(), library);
        CloudLogger.get().info("Loading library: {}", library.name());

        if (library.requiresUpdate()) {
            if (!library.download()) throw new RuntimeException("Failed to download lib " + library.name());
        }
    }

    @Override
    public void unload() {
        libraries.clear();
    }

    public void add(Library library) {
        libraries.put(library.name(), library);
        FileUtils.encodeJsonFile(PocketCloudPaths.storage().libraries().with("libraries.json").asPath(), libraries.values());
    }

    public Optional<Library> get(String name) {
        return Optional.ofNullable(libraries.get(name));
    }

    public List<Library> getFor(ServerSoftware software) {
        return libraries.values().stream()
                .filter(lib -> lib.isAvailableFor(software))
                .toList();
    }

    public Map<String, Library> getAll() {
        return Collections.unmodifiableMap(libraries);
    }
}