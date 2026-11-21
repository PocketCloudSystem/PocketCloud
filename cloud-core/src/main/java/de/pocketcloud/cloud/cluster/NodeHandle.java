package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.common.concurrent.Promise;

import java.util.List;
import java.util.Optional;

/**
 * A machine that runs servers. Everything that touches processes, ports or the
 * files of a running server goes through a node, so the rest of the cloud does not
 * care whether a server runs on this machine or on another one.
 * <p>
 * Today there is only the {@link LocalNodeHandle}. Remote nodes will implement the
 * same interface by sending cluster packets.
 */
public interface NodeHandle {

    String name();

    /**
     * Whether this node is the machine the current cloud process runs on.
     */
    boolean local();

    /**
     * Reserves a free port for a server of the given type on this node.
     *
     * @return the port, or a value {@code <= 0} if no port is available
     */
    int reservePort(TemplateType type);

    void releasePort(int port);

    /**
     * Copies bridge, templates, groups and software configs into the server's directory
     * and replaces the placeholders.
     */
    Promise<Void> prepare(CloudServer server);

    /**
     * Starts the server process.
     *
     * @return the process id, if the start method already knows it
     */
    Promise<Optional<Long>> boot(CloudServer server);

    Promise<Optional<Long>> lookupPid(CloudServer server);

    boolean isProcessAlive(CloudServer server);

    /**
     * Whether a process with the server's pid still exists (it may be a zombie).
     */
    boolean processExists(CloudServer server);

    void kill(CloudServer server);

    /**
     * Copies the savable files of the server back into its template.
     */
    Promise<Void> saveFiles(CloudServer server);

    Optional<List<String>> readLogs(CloudServer server);
}
