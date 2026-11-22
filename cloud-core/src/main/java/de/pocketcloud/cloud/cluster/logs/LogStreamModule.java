package de.pocketcloud.cloud.cluster.logs;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.ClusterModule;
import de.pocketcloud.cloud.cluster.Peer;
import de.pocketcloud.cloud.cluster.logs.packet.NodeLogLinesPacket;
import de.pocketcloud.cloud.cluster.logs.packet.NodeLogSubscribePacket;
import de.pocketcloud.cloud.cluster.logs.packet.NodeLogSubscribeResultPacket;
import de.pocketcloud.cloud.cluster.logs.packet.NodeLogUnsubscribePacket;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.network.packet.CloudPacket;
import io.netty.channel.Channel;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * Live log streaming of servers on other nodes ({@code server screen}, cluster spec: NodeLogLines).
 * <p>
 * Subscriber: {@link #subscribe(CloudServer, int)} sends {@link NodeLogSubscribePacket} to the owning node and
 * renews it every {@value #RENEW_MS} ms; closing the last subscription of a server sends {@link NodeLogUnsubscribePacket}.
 * After a reconnect to the owning node the subscriptions are renewed right away.
 * <p>
 * Owning node: tails the log file of the server (one reader per server, shared by all subscribers) and pushes new
 * lines every {@value #TICK_MS} ms in batches ({@link NodeLogLinesPacket}). Streaming to a node stops when it
 * unsubscribes, does not renew within {@value #LEASE_MS} ms or disconnects; streaming of a server stops
 * (with {@code ended = true}) when the server is gone.
 */
public final class LogStreamModule implements ClusterModule {

    static final long TICK_MS = 250;
    static final long RENEW_MS = 15_000;
    static final long LEASE_MS = 45_000;
    private static final int MAX_BACKLOG = 1_000;
    private static final int BATCH_CHARS = 400_000;
    private static final int BATCH_LINES = 2_000;
    private static final int READ_LIMIT = 1 << 20;
    private static final int BACKLOG_BYTES = 256 * 1024;

    private ClusterManager cluster;
    private ScheduledExecutorService executor;

    /** Owner side: server name → tail. Only touched on {@link #executor}. */
    private final Map<String, Tail> tails = new HashMap<>();
    /** Subscriber side: server name → local subscriptions. */
    private final Map<String, List<Subscription>> subscriptions = new ConcurrentHashMap<>();

    public static Optional<LogStreamModule> get() {
        ClusterManager cluster = PocketCloud.instance().cluster();
        if (cluster == null || !cluster.clustered()) return Optional.empty();
        for (ClusterModule module : cluster.modules()) {
            if (module instanceof LogStreamModule m && m.executor != null && !m.executor.isShutdown()) return Optional.of(m);
        }
        return Optional.empty();
    }

    @Override
    public Collection<Class<? extends Packet>> packets() {
        return List.of(NodeLogSubscribePacket.class, NodeLogSubscribeResultPacket.class, NodeLogUnsubscribePacket.class, NodeLogLinesPacket.class);
    }

    @Override
    public void start(ClusterManager cluster) {
        this.cluster = cluster;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PocketCloud-LogStream");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(safe(this::pump), TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
        executor.scheduleWithFixedDelay(safe(this::renewAll), RENEW_MS, RENEW_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (executor != null) executor.shutdownNow();
        for (List<Subscription> list : subscriptions.values()) list.forEach(s -> s.ended = true);
        subscriptions.clear();
    }

    private static Runnable safe(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                CloudLogger.get().exception("Log stream task failed", t);
            }
        };
    }

    // ------------------------------------------------------------------ subscriber side

    /**
     * A live view of the log of a server on another node. Lines arrive in {@link #poll()}.
     */
    public final class Subscription {
        private final String serverName;
        private final String node;
        private final Queue<String> lines = new ConcurrentLinkedQueue<>();
        private volatile boolean ended;
        private volatile String error;

        private Subscription(String serverName, String node) {
            this.serverName = serverName;
            this.node = node;
        }

        public String poll() {
            return lines.poll();
        }

        public boolean hasLines() {
            return !lines.isEmpty();
        }

        /** The server stopped (or the module shut down): no more lines will come. */
        public boolean ended() {
            return ended;
        }

        /** Why the owning node refused or did not answer the subscription, or null. */
        public String error() {
            return error;
        }

        public String node() {
            return node;
        }

        public void close() {
            ended = true;
            List<Subscription> list = subscriptions.get(serverName);
            if (list == null) return;
            boolean last;
            synchronized (list) {
                list.remove(this);
                last = list.stream().noneMatch(s -> s.node.equals(node));
                if (list.isEmpty()) subscriptions.remove(serverName, list);
            }
            if (last) cluster.send(node, new NodeLogUnsubscribePacket(serverName));
        }
    }

    /**
     * Subscribes to the live log of a server of another node.
     *
     * @param backlog number of past lines to receive first
     */
    public Subscription subscribe(CloudServer server, int backlog) {
        Subscription subscription = new Subscription(server.name(), server.nodeName());
        List<Subscription> list = subscriptions.computeIfAbsent(server.name(), _ -> new ArrayList<>());
        synchronized (list) {
            list.add(subscription);
        }
        NodeLogSubscribePacket request = new NodeLogSubscribePacket(server.name(), Math.min(MAX_BACKLOG, Math.max(0, backlog)));
        request.then(response -> {
            if (response instanceof NodeLogSubscribeResultPacket result && result.isSuccess()) {
                List<String> history = new ArrayList<>(result.getLines());
                // Lines that were pushed before the answer was processed belong after the backlog.
                List<String> early = new ArrayList<>();
                String line;
                while ((line = subscription.lines.poll()) != null) early.add(line);
                subscription.lines.addAll(history);
                subscription.lines.addAll(early);
            } else {
                subscription.error = response instanceof NodeLogSubscribeResultPacket result ? result.getError() : "unexpected response";
            }
        }).failure((_, ex, reason) -> subscription.error = ex != null ? String.valueOf(ex.getMessage()) : "no answer (" + reason + ")");
        if (!cluster.send(server.nodeName(), request)) subscription.error = "node " + server.nodeName() + " is not connected";
        return subscription;
    }

    /** Keeps the subscriptions of this node alive on the owning nodes. */
    private void renewAll() {
        for (Map.Entry<String, List<Subscription>> entry : subscriptions.entrySet()) {
            Set<String> nodes = new HashSet<>();
            synchronized (entry.getValue()) {
                for (Subscription s : entry.getValue()) if (!s.ended) nodes.add(s.node);
            }
            for (String node : nodes) renew(node, entry.getKey());
        }
    }

    private void renew(String node, String serverName) {
        NodeLogSubscribePacket request = new NodeLogSubscribePacket(serverName, 0);
        request.then(response -> {
            if (response instanceof NodeLogSubscribeResultPacket result && !result.isSuccess()) endLocal(serverName, node, result.getError());
        }).failure((_, _, _) -> {});
        cluster.send(node, request);
    }

    private void endLocal(String serverName, String node, String error) {
        List<Subscription> list = subscriptions.get(serverName);
        if (list == null) return;
        synchronized (list) {
            for (Subscription s : list) {
                if (!s.node.equals(node)) continue;
                s.ended = true;
                if (error != null) s.error = error;
            }
        }
    }

    private void onLines(String from, NodeLogLinesPacket packet) {
        List<Subscription> list = subscriptions.get(packet.getServerName());
        boolean anyone = false;
        if (list != null) {
            synchronized (list) {
                for (Subscription s : list) {
                    if (!s.node.equals(from) || s.ended) continue;
                    anyone = true;
                    if (packet.getLines() != null) s.lines.addAll(packet.getLines());
                    if (packet.isEnded()) s.ended = true;
                }
            }
        }
        // Nobody here wants these lines anymore (e.g. a lost unsubscribe): tell the owning node.
        if (!anyone && !packet.isEnded()) cluster.send(from, new NodeLogUnsubscribePacket(packet.getServerName()));
    }

    @Override
    public void onPeerConnected(String name) {
        // Lines sent while the connection was down are lost; renew so the owning node streams to the new connection.
        if (executor == null) return;
        executor.execute(() -> {
            for (Map.Entry<String, List<Subscription>> entry : subscriptions.entrySet()) {
                boolean affected;
                synchronized (entry.getValue()) {
                    affected = entry.getValue().stream().anyMatch(s -> !s.ended && s.node.equals(name));
                }
                if (affected) renew(name, entry.getKey());
            }
        });
    }

    // ------------------------------------------------------------------ owning node side

    private static final class Tail {
        final String serverName;
        final UUID uuid;
        final Path file;
        /** Always at the start of a line: incomplete lines are read again on the next tick. */
        long position;
        /** Kept open, so the last lines can still be read after the server directory was deleted on stop. */
        RandomAccessFile file_;
        final Map<String, Long> subscribers = new HashMap<>(); // node → last renewal

        Tail(CloudServer server) {
            this.serverName = server.name();
            this.uuid = server.uuid();
            this.file = server.logFilePath();
        }
    }

    private void handleSubscribe(String from, Channel channel, NodeLogSubscribePacket request) {
        executor.execute(safe(() -> {
            CloudServer server = localServer(request.getServerName());
            if (server == null) {
                cluster.respond(channel, request, new NodeLogSubscribeResultPacket(false, "server " + request.getServerName() + " does not run on node " + cluster.nodeName(), null));
                return;
            }
            Tail tail = tails.get(server.name());
            if (tail != null && !tail.uuid.equals(server.uuid())) {
                end(tail);
                tail = null;
            }
            boolean created = tail == null;
            if (created) {
                tail = new Tail(server);
                tail.position = Files.isRegularFile(tail.file) ? safeSize(tail.file) : 0;
                tails.put(server.name(), tail);
            }

            List<String> backlog = List.of();
            try {
                // Reading starts at a line boundary; the lines before it are the backlog.
                long[] boundary = new long[1];
                List<String> lines = readBefore(tail.file, tail.position, request.getBacklog(), boundary);
                if (created) tail.position = boundary[0];
                backlog = lines;
            } catch (IOException e) {
                CloudLogger.get().debug("Cannot read the log backlog of {}: {}", server.name(), e.getMessage());
            }

            boolean isNew = tail.subscribers.put(from, System.currentTimeMillis()) == null;
            if (isNew) CloudLogger.get().info("Streaming the log of §b{} §rto node §b{}§r.", server.name(), from);
            cluster.respond(channel, request, new NodeLogSubscribeResultPacket(true, null, backlog));
        }));
    }

    private void handleUnsubscribe(String from, NodeLogUnsubscribePacket packet) {
        executor.execute(() -> {
            Tail tail = tails.get(packet.getServerName());
            if (tail != null) removeSubscriber(tail, from, "unsubscribed");
        });
    }

    @Override
    public void onPeerGone(String name) {
        if (executor == null) return;
        executor.execute(() -> {
            for (Tail tail : List.copyOf(tails.values())) removeSubscriber(tail, name, "node is gone");
        });
    }

    private void removeSubscriber(Tail tail, String node, String reason) {
        if (tail.subscribers.remove(node) == null) return;
        CloudLogger.get().info("Stopped streaming the log of §b{} §rto node §b{} §8(§r{}§8)§r.", tail.serverName, node, reason);
        if (tail.subscribers.isEmpty()) drop(tail);
    }

    private void drop(Tail tail) {
        tails.remove(tail.serverName, tail);
        if (tail.file_ != null) {
            try {
                tail.file_.close();
            } catch (IOException ignored) {
            }
            tail.file_ = null;
        }
    }

    /** Reads new lines of every tailed log and pushes them to the subscribers. */
    private void pump() {
        long now = System.currentTimeMillis();
        for (Tail tail : List.copyOf(tails.values())) {
            for (Map.Entry<String, Long> sub : List.copyOf(tail.subscribers.entrySet())) {
                Peer peer = cluster.peer(sub.getKey()).orElse(null);
                if (now - sub.getValue() > LEASE_MS) removeSubscriber(tail, sub.getKey(), "subscription not renewed");
                // A reconnecting subscriber renews its subscription right away (onPeerConnected).
                else if (peer == null || !peer.connected()) removeSubscriber(tail, sub.getKey(), "node disconnected");
            }
            if (!tails.containsKey(tail.serverName)) continue;

            List<String> lines = readNew(tail);
            CloudServer server = localServer(tail.serverName);
            boolean gone = server == null || !server.uuid().equals(tail.uuid);
            if (!lines.isEmpty() || gone) {
                for (String node : tail.subscribers.keySet()) sendBatches(node, tail.serverName, lines, gone);
            }
            if (gone) {
                CloudLogger.get().info("Stopped streaming the log of §b{}§r: the server stopped.", tail.serverName);
                drop(tail);
            }
        }
    }

    private void end(Tail tail) {
        for (String node : tail.subscribers.keySet()) cluster.send(node, new NodeLogLinesPacket(tail.serverName, List.of(), true));
        drop(tail);
    }

    private void sendBatches(String node, String serverName, List<String> lines, boolean ended) {
        List<String> batch = new ArrayList<>();
        int chars = 0;
        for (String line : lines) {
            if (!batch.isEmpty() && (chars + line.length() > BATCH_CHARS || batch.size() >= BATCH_LINES)) {
                cluster.send(node, new NodeLogLinesPacket(serverName, batch, false));
                batch = new ArrayList<>();
                chars = 0;
            }
            batch.add(line.length() > BATCH_CHARS ? line.substring(0, BATCH_CHARS) : line);
            chars += Math.min(line.length(), BATCH_CHARS);
        }
        if (!batch.isEmpty() || ended) cluster.send(node, new NodeLogLinesPacket(serverName, batch, ended));
    }

    private List<String> readNew(Tail tail) {
        try {
            if (tail.file_ == null) {
                if (!Files.isRegularFile(tail.file)) return List.of();
                tail.file_ = new RandomAccessFile(tail.file.toFile(), "r");
            }
            RandomAccessFile raf = tail.file_;
            long length = raf.length();
            if (length < tail.position) tail.position = 0; // truncated or replaced
            if (length == tail.position) return List.of();
            int size = (int) Math.min(READ_LIMIT, length - tail.position);
            byte[] data = new byte[size];
            raf.seek(tail.position);
            raf.readFully(data);
            int lastNewline = lastIndexOf(data, size);
            if (lastNewline < 0) {
                // One huge line without end: pass it on in pieces instead of waiting forever.
                if (size < READ_LIMIT) return List.of();
                tail.position += size;
                return List.of(new String(data, StandardCharsets.UTF_8));
            }
            tail.position += lastNewline + 1;
            return split(data, 0, lastNewline);
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * The last {@code count} complete lines before {@code end}. {@code boundary[0]} is set to the end of the
     * last complete line (where streaming continues).
     */
    private static List<String> readBefore(Path file, long end, int count, long[] boundary) throws IOException {
        boundary[0] = end;
        if (end <= 0 || !Files.isRegularFile(file)) {
            boundary[0] = 0;
            return List.of();
        }
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long start = Math.max(0, end - BACKLOG_BYTES);
            int size = (int) (end - start);
            byte[] data = new byte[size];
            raf.seek(start);
            raf.readFully(data);
            int lastNewline = lastIndexOf(data, size);
            if (lastNewline < 0) {
                boundary[0] = start;
                return List.of();
            }
            boundary[0] = start + lastNewline + 1;
            if (count <= 0) return List.of();
            List<String> lines = split(data, 0, lastNewline);
            if (start > 0 && !lines.isEmpty()) lines.removeFirst(); // probably cut
            return new ArrayList<>(lines.subList(Math.max(0, lines.size() - count), lines.size()));
        }
    }

    private static int lastIndexOf(byte[] data, int size) {
        for (int i = size - 1; i >= 0; i--) if (data[i] == '\n') return i;
        return -1;
    }

    /** Lines of {@code data[from, to)} ({@code to} is a newline), without line terminators. */
    private static List<String> split(byte[] data, int from, int to) {
        List<String> lines = new ArrayList<>();
        int start = from;
        for (int i = from; i <= to; i++) {
            if (data[i] != '\n') continue;
            int end = i > start && data[i - 1] == '\r' ? i - 1 : i;
            lines.add(new String(data, start, end - start, StandardCharsets.UTF_8));
            start = i + 1;
        }
        return lines;
    }

    private static long safeSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    private static CloudServer localServer(String name) {
        return PocketCloud.instance().servers().get(name)
                .filter(s -> s instanceof CloudServer cs && !cs.remote())
                .map(CloudServer.class::cast)
                .orElse(null);
    }

    // ------------------------------------------------------------------ packets

    @Override
    public boolean onPacket(String from, Channel channel, CloudPacket packet) {
        switch (packet) {
            case NodeLogSubscribePacket request -> handleSubscribe(from, channel, request);
            case NodeLogUnsubscribePacket unsubscribe -> handleUnsubscribe(from, unsubscribe);
            case NodeLogLinesPacket lines -> onLines(from, lines);
            default -> {
                return false;
            }
        }
        return true;
    }
}
