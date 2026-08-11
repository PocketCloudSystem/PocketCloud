package de.pocketcloud.bridge.provider;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.provider.write.IWriteSoftwareProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SoftwareProvider implements IWriteSoftwareProvider {

    private final Map<String, IServerSoftware> softwares = new ConcurrentHashMap<>();

    @Override
    public void register(IServerSoftware software, boolean override) {
        if (softwares.containsKey(software.name()) && !override) return;
        softwares.put(software.name(), software);
    }

    @Override
    public void unregister(IServerSoftware software) {
        softwares.remove(software.name());
    }

    @Override
    public boolean check(String name) {
        return softwares.containsKey(name);
    }

    @Override
    public Optional<IServerSoftware> get(String name) {
        return Optional.ofNullable(softwares.get(name));
    }

    @Override
    public int softwareCount() {
        return softwares.size();
    }

    @Override
    public Collection<IServerSoftware> getAll() {
        return softwares.values().stream().toList();
    }
}