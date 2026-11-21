package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.common.concurrent.Promise;

import java.util.List;
import java.util.Optional;

/**
 * A node on another machine. Servers of such a node are pure data objects here:
 * their process, files and ports are handled by the owning node. Stops are forwarded to it.
 */
public final class RemoteNodeHandle implements NodeHandle {

    private final String name;

    public RemoteNodeHandle(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean local() {
        return false;
    }

    @Override
    public int reservePort(TemplateType type) {
        throw new UnsupportedOperationException("Ports are reserved by the node " + name + " itself");
    }

    @Override
    public void releasePort(int port) {
        // The owning node releases its ports.
    }

    @Override
    public Promise<Void> prepare(CloudServer server) {
        return Promise.rejected(new UnsupportedOperationException(server.name() + " is prepared on node " + name));
    }

    @Override
    public Promise<Optional<Long>> boot(CloudServer server) {
        return Promise.rejected(new UnsupportedOperationException(server.name() + " is started on node " + name));
    }

    @Override
    public Promise<Optional<Long>> lookupPid(CloudServer server) {
        return Promise.resolved(Optional.empty());
    }

    @Override
    public boolean isProcessAlive(CloudServer server) {
        return !server.status().isOffline();
    }

    @Override
    public boolean processExists(CloudServer server) {
        return !server.status().isOffline();
    }

    @Override
    public void kill(CloudServer server) {
        PocketCloud.instance().cluster().forwardStop(name, server.name(), true);
    }

    @Override
    public Promise<Void> saveFiles(CloudServer server) {
        return Promise.rejected(new UnsupportedOperationException(server.name() + " is saved by node " + name));
    }

    @Override
    public Optional<List<String>> readLogs(CloudServer server) {
        return Optional.empty();
    }
}
