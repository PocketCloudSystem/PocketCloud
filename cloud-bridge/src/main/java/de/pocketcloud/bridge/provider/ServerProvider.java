package de.pocketcloud.bridge.provider;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.language.LanguageKey;
import de.pocketcloud.api.provider.write.IWriteServerProvider;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.component.CloudServer;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.network.packet.RequestPacket;
import de.pocketcloud.network.packet.RequestPacketFailureReason;
import de.pocketcloud.network.packet.impl.request.ServerSaveRequestPacket;
import de.pocketcloud.network.packet.impl.request.ServerStartRequestPacket;
import de.pocketcloud.network.packet.impl.request.ServerStopRequestPacket;
import de.pocketcloud.network.packet.impl.response.ServerSaveResponsePacket;
import de.pocketcloud.network.packet.impl.response.ServerStartResponsePacket;
import de.pocketcloud.network.packet.impl.response.ServerStopResponsePacket;
import de.pocketcloud.shared.event.server.*;
import de.pocketcloud.shared.network.packet.type.ActionFailureReason;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ServerProvider implements IWriteServerProvider {

    private final Map<String, ICloudServer> servers = new ConcurrentHashMap<>();

    @Override
    public void add(ICloudServer server) {
        boolean verified = CloudBridge.instance().status().isVerified();
        if (servers.containsKey(server.name())) {
            CloudServer localServer = (CloudServer) servers.get(server.name());
            ServerStatus oldStatus = localServer.status();
            localServer.syncIn(server);
            if (verified && oldStatus != localServer.status())
                CloudAPI.instance().events().call(new ServerChangedStatusEvent(localServer, localServer.status(), server.status()));
            if (verified && oldStatus.isOnline() && !localServer.status().isOnline() && server.verificationStatus().isVerified())
                CloudAPI.instance().events().call(new ServerVerifiedEvent(localServer));
        } else {
            servers.put(server.name(), server);
            if (verified && server.status().isStarting())
                CloudAPI.instance().events().call(new ServerStartingEvent(server));
        }
    }

    @Override
    public void remove(ICloudServer server) {
        CloudServer localServer = (CloudServer) servers.get(server.name());
        if (CloudBridge.instance().status().isVerified()) {
            CloudAPI.instance().events().call(new ServerDisconnectedEvent(localServer));
            if (server.verificationStatus().isDenied())
                CloudAPI.instance().events().call(new ServerVerificationDeniedEvent(localServer));
        }

        servers.remove(server.name());
    }

    @Override
    public Promise<Collection<String>> start(ITemplate template, int count) {
        Promise<Collection<String>> promise = new Promise<>();
        ServerStartRequestPacket.create(template.name(), count).sendRequest().then(res -> {
            if (res.getErrorReason() == ActionFailureReason.NONE) {
                promise.resolve(res.getStartedServers());
            } else {
                if (res.getErrorReason() == ActionFailureReason.REQUEST_TIMEOUT) {
                    promise.reject(LanguageKey.INGAME_REQUEST_TIMED_OUT.translate(res.getRequestId(), res.getName()));
                } else promise.reject(res.getErrorReason().toMessage(template.name()));
            }
        }, ServerStartResponsePacket.class).failure((req, t, r) -> {
            if (r == RequestPacketFailureReason.REQUEST_TIMEOUT) {
                promise.reject(LanguageKey.INGAME_REQUEST_TIMED_OUT.translate(req.getRequestId(), req.getName()));
            } else {
                promise.reject(t);
            }
        });
        return promise;
    }

    @Override
    public Promise<Void> save(ICloudServer server) {
        Promise<Void> promise = new Promise<>();
        ServerSaveRequestPacket.create(server.name()).sendRequest().then(res -> {
            if (res.getErrorReason() == ActionFailureReason.NONE) {
                promise.resolve(null);
            } else {
                if (res.getErrorReason() == ActionFailureReason.REQUEST_TIMEOUT) {
                    promise.reject(LanguageKey.INGAME_REQUEST_TIMED_OUT.translate(res.getRequestId(), res.getName()));
                } else promise.reject(res.getErrorReason().toMessage(server.name()));
            }
        }, ServerSaveResponsePacket.class).failure((req, t, r) -> {
            defaultPromiseExceptionally(promise, req, t, r);
        });
        return promise;
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(ICloudServer server, boolean force) {
        return stop(server.name(), force);
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(ITemplate template, boolean force) {
        return stop(template.name(), force);
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(IServerGroup group, boolean force) {
        return stop(group.name(), force);
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(TemplateType type, boolean force) {
        return stop(type.name(), force);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Promise<Collection<ICloudServer>> stop(String name, boolean force) {
        Promise<Collection<ICloudServer>> promise = new Promise<>();
        ServerStopRequestPacket.create(name, force).sendRequest().then(res -> {
            if (res.getErrorReason() == ActionFailureReason.NONE) {
                promise.resolve((Collection<ICloudServer>) res.getAffectedServers());
            } else promise.reject(res.getErrorReason().toMessage(name));
        }, ServerStopResponsePacket.class).failure((req, t, r) -> {
            defaultPromiseExceptionally(promise, req, t, r);
        });
        return promise;
    }

    @Override
    public Promise<Collection<ICloudServer>> stopAll(boolean force) {
        return stop("stop-all", force);
    }

    private void defaultPromiseExceptionally(Promise<?> promise, RequestPacket req, Throwable t, RequestPacketFailureReason r) {
        if (r == RequestPacketFailureReason.REQUEST_TIMEOUT) {
            promise.reject(LanguageKey.INGAME_REQUEST_TIMED_OUT.translate(req.getRequestId(), req.getName()));
        } else {
            promise.reject(t);
        }
    }

    @Override
    public boolean check(String name) {
        return servers.containsKey(name);
    }

    @Override
    public boolean check(UUID uuid) {
        return servers.values().stream().anyMatch(s -> s.uuid().equals(uuid));
    }

    @Override
    public boolean checkCapacity(ITemplate template) {
        return query(ServerSearchQuery.create().ofTemplate(template)).size() < template.settings().maxServerCount();
    }

    @Override
    public Optional<ICloudServer> get(String name) {
        return Optional.ofNullable(servers.get(name));
    }

    @Override
    public Optional<ICloudServer> get(UUID uuid) {
        return servers.values().stream().filter(s -> s.uuid().equals(uuid)).findFirst();
    }

    @Override
    public ICloudServer current() {
        return Optional.ofNullable(servers.get(CloudBridge.instance().environmentConfig().localServerName())).orElseThrow(() -> new IllegalStateException("Current server should not be null, called too early?"));
    }

    @Override
    public Collection<ICloudServer> query(ServerSearchQuery searchQuery) {
        return servers.values().stream()
                .filter(searchQuery::matches)
                .toList();
    }

    @Override
    public Collection<ICloudServer> query(Consumer<ServerSearchQuery> queryConsumer) {
        ServerSearchQuery searchQuery = ServerSearchQuery.create();
        queryConsumer.accept(searchQuery);
        return servers.values().stream()
                .filter(searchQuery::matches)
                .toList();
    }

    @Override
    public int serverCount() {
        return servers.size();
    }

    @Override
    public Collection<ICloudServer> getAll() {
        return servers.values().stream().toList();
    }
}