package de.pocketcloud.cloud.server;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Restarts the servers of a template one after another, so the template never runs with fewer servers:
 * a new server is started (and gets the current template version) before an old one stops. The players of the
 * stopped server are sent to a lobby by the proxy's fallback handler. Works from any node: starts and stops go
 * through the cluster like the console commands.
 */
public final class RollingRestart {

    private static final long STEP_TIMEOUT_MS = 180_000;
    private static final long REPLACEMENT_WAIT_MS = 10_000;
    private static final long REACHABLE_WAIT_MS = 60_000;

    private static final Set<String> running = ConcurrentHashMap.newKeySet();
    /** Templates that were updated again during their restart: restarted once more afterwards. */
    private static final Set<String> again = ConcurrentHashMap.newKeySet();

    private RollingRestart() {
    }

    /** @return false if a rolling restart of this template is already running */
    public static boolean start(ITemplate template, Consumer<String> log) {
        if (!running.add(template.name())) return false;
        Thread.ofVirtual().name("rolling-restart-" + template.name()).start(() -> {
            try {
                run(template, log);
            } catch (Exception e) {
                log.accept("§cRolling restart of §b" + template.name() + " §cstopped§8: §c" + e.getMessage());
            } finally {
                running.remove(template.name());
                if (again.remove(template.name())) start(template, log);
            }
        });
        return true;
    }

    /** After a template update (cluster.restartOnUpdate): restart now, or once more after the running restart. */
    public static void onTemplateUpdated(String templateName) {
        if (!PocketCloud.instance().config().cluster().restartOnUpdate().contains(templateName)) return;
        Optional<ITemplate> template = PocketCloud.instance().templates().get(templateName);
        if (template.isEmpty()) return;
        if (!start(template.get(), CloudLogger.get()::info)) again.add(templateName);
    }

    private static void run(ITemplate template, Consumer<String> log) throws InterruptedException {
        CloudServerManager servers = PocketCloud.instance().servers();
        // by uuid: a replacement can get the name (id) of a server that was stopped before
        List<ICloudServer> old = servers(template).stream().sorted(Comparator.comparing(ICloudServer::name)).toList();
        if (old.isEmpty()) {
            log.accept("Template §b" + template.name() + " §rhas no running servers.");
            return;
        }
        log.accept("Restarting §b" + old.size() + " §rserver(s) of §b" + template.name() + " §rone after another...");
        Set<UUID> known = new HashSet<>(old.stream().map(ICloudServer::uuid).toList()); // everything else is a replacement
        for (ICloudServer oldServer : old) {
            String name = oldServer.name();
            UUID uuid = oldServer.uuid();
            Optional<ICloudServer> server = find(template, uuid);
            if (server.isEmpty()) continue;
            int onlineBefore = online(template);

            if (!template.settings().staticServers() && servers(template).size() < template.settings().maxServerCount()) {
                servers.start(template, 1);
                UUID fresh = waitFor(() -> servers(template).stream()
                        .filter(s -> !known.contains(s.uuid()) && s.status().isOnline(true))
                        .map(ICloudServer::uuid).findFirst(), "a new server of " + template.name() + " to start");
                known.add(fresh);
                waitReachable(template, fresh);
                servers.stop(server.get(), false);
                waitFor(() -> find(template, uuid).isEmpty() ? Optional.of(name) : Optional.empty(), name + " to stop");
            } else {
                // static servers or maxServerCount reached: stop first, then wait for the replacement
                int countBefore = servers(template).size();
                servers.stop(server.get(), false);
                waitFor(() -> find(template, uuid).isEmpty() ? Optional.of(name) : Optional.empty(), name + " to stop");
                long deadline = System.currentTimeMillis() + REPLACEMENT_WAIT_MS;
                while (servers(template).size() < countBefore && System.currentTimeMillis() < deadline) Thread.sleep(500);
                if (servers(template).size() < countBefore) servers.start(template, 1); // the auto start did not replace it
                waitFor(() -> online(template) >= onlineBefore ? Optional.of(name) : Optional.empty(), "the replacement of " + name + " to start");
                servers(template).forEach(s -> known.add(s.uuid()));
            }
            log.accept("Restarted §b" + name + "§r.");
        }
        log.accept("All servers of §b" + template.name() + " §rwere §arestarted§r.");
    }

    /**
     * NetherNet servers report ONLINE a moment before their signaling (TCP on the server port) accepts connections;
     * players moved there earlier get "connection refused". Waits for the port, at most {@link #REACHABLE_WAIT_MS}.
     */
    private static void waitReachable(ITemplate template, UUID uuid) throws InterruptedException {
        String transport = template.serverSoftware().config().transport();
        if (!"nethernet".equalsIgnoreCase(transport)) return;
        Optional<ICloudServer> server = find(template, uuid);
        if (server.isEmpty()) return;
        InetSocketAddress address = new InetSocketAddress(server.get().data().address(), server.get().data().port());
        long deadline = System.currentTimeMillis() + REACHABLE_WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(address, 1000);
                return;
            } catch (IOException e) {
                Thread.sleep(500);
            }
        }
        CloudLogger.get().debug("{} is ONLINE but {} does not accept connections yet, continuing", server.get().name(), address);
    }

    private static Collection<ICloudServer> servers(ITemplate template) {
        return PocketCloud.instance().servers().query(ServerSearchQuery.create().ofTemplate(template));
    }

    private static Optional<ICloudServer> find(ITemplate template, UUID uuid) {
        return servers(template).stream().filter(s -> s.uuid().equals(uuid)).findFirst();
    }

    private static int online(ITemplate template) {
        return (int) servers(template).stream().filter(s -> s.status().isOnline(true)).count();
    }

    private static <T> T waitFor(Supplier<Optional<T>> check, String what) throws InterruptedException {
        long deadline = System.currentTimeMillis() + STEP_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Optional<T> result = check.get();
            if (result.isPresent()) return result.get();
            Thread.sleep(500);
        }
        throw new IllegalStateException("timed out waiting for " + what);
    }
}
