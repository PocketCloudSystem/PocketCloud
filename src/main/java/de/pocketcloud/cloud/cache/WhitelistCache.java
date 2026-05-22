package de.pocketcloud.cloud.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WhitelistCache implements LocalCache<String> {

    private final Set<String> whitelist = new HashSet<>();

    @Override
    public void syncIn(List<String> cache) {
        whitelist.clear();
        whitelist.addAll(cache);
    }

    @Override
    public void syncOut() {

    }

    @Override
    public void add(String element) {
        whitelist.add(element);
    }

    @Override
    public void remove(String element) {
        whitelist.remove(element);
    }

    @Override
    public boolean contains(String element) {
        return whitelist.contains(element);
    }

    @Override
    public Collection<String> getAll() {
        return whitelist.stream().toList();
    }
}