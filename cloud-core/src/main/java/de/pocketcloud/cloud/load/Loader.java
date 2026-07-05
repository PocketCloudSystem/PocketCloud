package de.pocketcloud.cloud.load;

import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.common.lifecycle.Loadable;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public final class Loader {

    /**
     * Preloaders are basically loaders who load something before the actual cloud startup happens. They will not be reloaded.
     */
    private final Map<String, Loadable> preLoaders = new HashMap<>();
    private final Map<String, Loadable> loadableList = new HashMap<>();
    @Getter
    private boolean reloading = false;

    public Loader register(Loadable loadable) {
        if (loadableList.containsKey(loadable.getClass().getName())) throw new IllegalStateException("Loadable already exists");
        loadableList.put(loadable.getClass().getName(), loadable);
        return this;
    }

    public Loader registerPre(Loadable loadable) {
        if (preLoaders.containsKey(loadable.getClass().getName())) throw new IllegalStateException("Loadable already exists");
        preLoaders.put(loadable.getClass().getName(), loadable);
        return this;
    }

    public Loader registerPreAll(Loadable... loadables) {
        for (Loadable loadable : loadables) registerPre(loadable);
        return this;
    }

    public Loader registerAll(Loadable... loadables) {
        for (Loadable loadable : loadables) register(loadable);
        return this;
    }

    public Loader unregister(Class<Loadable> loadable) {
        loadableList.remove(loadable.getName());
        return this;
    }

    public Loader unregister(Loadable loadable) {
        loadableList.remove(loadable.getClass().getName());
        return this;
    }

    public Loader unregisterPre(Class<Loadable> loadable) {
        preLoaders.remove(loadable.getName());
        return this;
    }

    public Loader unregisterPre(Loadable loadable) {
        preLoaders.remove(loadable.getClass().getName());
        return this;
    }

    public void preloadAll() {
        for (Loadable loadable : preLoaders.values()) {
            Benchmark.startTiming("preload_" + loadable.getClass().getName());
            loadable.load();
            Benchmark.stopTiming("preload_" + loadable.getClass().getName());
        }
    }

    public void loadAll() {
        for (Loadable loadable : loadableList.values()) {
            Benchmark.startTiming("load_" + loadable.getClass().getName());
            loadable.load();
            Benchmark.stopTiming("load_" + loadable.getClass().getName());
        }
    }

    public void unloadAll() {
        for (Loadable loadable : loadableList.values()) {
            Benchmark.startTiming("unload_" + loadable.getClass().getName());
            loadable.unload();
            Benchmark.stopTiming("unload_" + loadable.getClass().getName());
        }
    }

    public void reload() {
        if (reloading) return;
        reloading = true;
        unloadAll();
        loadAll();
        reloading = false;
    }
}