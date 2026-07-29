package de.pocketcloud.cloud.server.crash;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.crash.impl.WaterdogPowerNukkitCrashHandler;
import de.pocketcloud.common.lifecycle.Loadable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CrashHandlerRegistry implements Loadable {

    private final Map<Class<?>, CrashHandler> crashHandlers = new HashMap<>();

    @Override
    public void load() {
        register(new WaterdogPowerNukkitCrashHandler());
    }

    @Override
    public void unload() {
        unregister(WaterdogPowerNukkitCrashHandler.class);
    }

    public void register(CrashHandler handler) {
        crashHandlers.put(handler.getClass(), handler);
    }

    public <T extends CrashHandler> void unregister(Class<T> handlerClass) {
        crashHandlers.remove(handlerClass);
    }

    public CrashData retrieveCrashData(CloudServer server) {
        List<CrashHandler> handlers = get(server.template().serverSoftware());
        for (CrashHandler handler : handlers) {
            CrashData data = handler.retrieveCrashData(server);
            if (data.crashed()) {
                return data;
            }
        }

        return CrashData.noCrash(server);
    }

    public boolean has(IServerSoftware software) {
        return crashHandlers.values().stream().anyMatch(h -> h.applicableSoftware().contains(software));
    }

    public List<CrashHandler> get(IServerSoftware software) {
        return crashHandlers.values().stream().filter(h -> h.applicableSoftware().contains(software)).toList();
    }

    public List<CrashHandler> getAll() {
        return crashHandlers.values().stream().toList();
    }
}