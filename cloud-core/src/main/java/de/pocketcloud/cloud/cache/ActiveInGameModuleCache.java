package de.pocketcloud.cloud.cache;

import de.pocketcloud.cloud.network.packet.impl.ModuleSyncPacket;
import de.pocketcloud.common.cache.LocalCache;

import java.util.*;

public final class ActiveInGameModuleCache implements LocalCache<String> {

    public static final String SIGN_MODULE = "sign_module";
    public static final String NPC_MODULE = "npc_module";
    public static final String HUB_COMMAND_MODULE = "hub_command_module";

    private final Set<String> enabledModules = new HashSet<>();

    @Override
    public void syncIn(List<String> cache) {
        enabledModules.clear();
        enabledModules.addAll(cache);
    }

    @Override
    public void syncOut() {
        ModuleSyncPacket.fromModuleCache().broadcastPacket();
    }

    @Override
    public void add(String element) {
        enabledModules.add(element);
    }

    @Override
    public void remove(String element) {
        enabledModules.remove(element);
    }

    public void set(String module, boolean enabled) {
        if (enabled) add(module);
        else remove(module);
    }

    @Override
    public boolean contains(String element) {
        return enabledModules.contains(element);
    }

    @Override
    public Collection<String> getAll() {
        return enabledModules.stream().toList();
    }
}