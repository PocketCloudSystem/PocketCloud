package de.pocketcloud.cloud.server.library;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.sync.SyncType;

import java.nio.file.Files;
import java.util.*;

public final class LibraryManager implements Loadable {

    public static final List<Library> DEFAULTS = List.of();

    private final Map<String, Library> libraries = new LinkedHashMap<>();

    public void load() {
        try {
            if (!Files.isRegularFile(PocketCloudPaths.storage().libraries().with("libraries.json").asPath()))
                FileUtils.filePutContents(
                        PocketCloudPaths.storage().libraries().with("libraries.json").asPath(),
                        FileUtils.PRETTY_GSON.toJson(DEFAULTS)
                );

            List<Library> libs = FileUtils.decodeJsonFile(PocketCloudPaths.storage().libraries().with("libraries.json").asPath(), new TypeToken<>() {});
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

    public SyncPacket buildSyncPacket(CloudServer server) {
        List<LinkedHashMap<String, String>> data = new ArrayList<>();
        for (Library lib : PocketCloud.instance().libraries().getAll().values()) {
            if (!lib.isAvailableFor(server.template().serverSoftware())) continue;
            LinkedHashMap<String, String> libData = new LinkedHashMap<>();
            libData.put("name", lib.name());
            libData.put("path", lib.directoryPath().toAbsolutePath().toString());
            libData.put("namespacePrefix", lib.namespacePrefix());
            libData.put("namespaceFolder", lib.namespaceFolder());
            data.add(libData);
        }

        return SyncPacket.create(SyncType.LIBRARIES, pData -> pData.write(data));
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

    public List<Library> getFor(IServerSoftware software) {
        return libraries.values().stream()
                .filter(lib -> lib.isAvailableFor(software))
                .toList();
    }

    public Map<String, Library> getAll() {
        return Collections.unmodifiableMap(libraries);
    }
}