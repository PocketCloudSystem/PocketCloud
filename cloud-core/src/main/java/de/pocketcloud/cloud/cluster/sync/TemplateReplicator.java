package de.pocketcloud.cloud.cluster.sync;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.ClusterModule;
import de.pocketcloud.cloud.cluster.Peer;
import de.pocketcloud.cloud.cluster.storage.TemplateLocks;
import de.pocketcloud.cloud.cluster.sync.packet.*;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.RequestPacket;
import de.pocketcloud.network.packet.ResponsePacket;
import io.netty.channel.Channel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

/**
 * Template replication (cluster spec, "Template-Replikation").
 * <p>
 * Every node keeps a full copy of {@code templates/<name>}, {@code templates/global/<type>}, {@code groups/<name>}
 * and, with the JSON provider, of {@code templates.json}/{@code groups.json} ("units"). The head owns the valid
 * version of every unit (number + manifest hash) and keeps a history of the last 10 versions.
 * <ul>
 *     <li>A WatchService reports local changes. After 3 s without events and an unchanged listing
 *     (size + mtime) the node sends a delta against the version it last took over to the head.</li>
 *     <li>The head merges it (same file changed concurrently on two nodes: the later change wins, the other
 *     version stays in the history, the console warns), fetches the files it does not have, bumps the version
 *     and announces it. Changes made on the head itself are taken over directly.</li>
 *     <li>Nodes whose copy differs fetch the manifest and only the files they do not have (256 KiB chunks),
 *     build the new version in a temp folder and swap it in under the template's write lock.</li>
 *     <li>After (re)connecting to the head and every 30 s a node compares all units with the head.</li>
 * </ul>
 * Runs only while the node is clustered; single nodes have no watcher and no overhead.
 */
public final class TemplateReplicator implements ClusterModule {

    public static final int CHUNK_SIZE = 256 * 1024;
    private static final long DEBOUNCE_MS = 3_000;
    private static final long RECHECK_MS = 1_000;
    private static final long RESYNC_MS = 30_000;
    private static final long PUSH_TIMEOUT_MS = 60_000;
    private static final long REQUEST_TIMEOUT_MS = 20_000;
    private static final String DELETED = "deleted";
    private static final Gson GSON = new Gson();

    private final Path directory = PocketCloudPaths.storage().internal().with("cluster").with("template-sync").asPath();
    private final VersionStore headVersions = new VersionStore(directory.resolve("head.json"));
    private final VersionStore applied = new VersionStore(directory.resolve("applied.json"));
    private final TemplateHistory history = new TemplateHistory(PocketCloudPaths.storage().internal().with("cluster").with("template-history").asPath());
    private final UnitScanner scanner = new UnitScanner();

    private final Map<String, Dirty> dirty = new ConcurrentHashMap<>();
    private final Map<String, Long> pushPending = new ConcurrentHashMap<>();
    /** Local manifest hash last sent to the head; the head's next version already contains these changes. */
    private final Map<String, String> pushedHashes = new ConcurrentHashMap<>();
    private final Map<String, HeadTemplateListPacket.Item> lastHeadVersions = new ConcurrentHashMap<>();
    private final Set<String> paused = ConcurrentHashMap.newKeySet();
    /** unit → (note, time): why its files are about to change (server saves), shown in the history. */
    private final Map<String, Map.Entry<String, Long>> notes = new ConcurrentHashMap<>();
    private volatile boolean pausedAll;

    private ClusterManager cluster;
    private ExecutorService syncExecutor;
    private ExecutorService serveExecutor;
    private TemplateWatcher watcher;
    private boolean jsonMeta;
    private volatile String knownHead;
    private volatile boolean headReady;
    private volatile long lastFullSync;
    private volatile boolean running;

    private static final class Dirty {
        volatile long lastEvent;
        volatile long checkedAt;
        volatile boolean queued;
        Map<String, UnitScanner.Stat> listing;
    }

    public static Optional<TemplateReplicator> get() {
        ClusterManager cluster = PocketCloud.instance().cluster();
        if (!cluster.clustered()) return Optional.empty();
        for (ClusterModule module : cluster.modules()) {
            if (module instanceof TemplateReplicator r && r.running) return Optional.of(r);
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------ module lifecycle

    @Override
    public Collection<Class<? extends Packet>> packets() {
        return List.of(NodeTemplateListRequestPacket.class, HeadTemplateListPacket.class, HeadTemplateVersionPacket.class,
                NodeTemplateManifestRequestPacket.class, HeadTemplateManifestPacket.class,
                NodeTemplateChunkRequestPacket.class, NodeTemplateChunkPacket.class,
                NodeTemplatePushPacket.class, HeadTemplatePushAckPacket.class,
                NodeTemplateAdminPacket.class, HeadTemplateAdminResultPacket.class);
    }

    @Override
    public void start(ClusterManager cluster) {
        this.cluster = cluster;
        this.jsonMeta = !"mysql".equalsIgnoreCase(PocketCloud.instance().config().provider());
        headVersions.load();
        applied.load();
        FileUtils.removeDirectory(directory.resolve("tmp"));
        FileUtils.removeDirectory(directory.resolve("staging"));

        syncExecutor = Executors.newSingleThreadExecutor(daemon("PocketCloud-TemplateSync"));
        serveExecutor = Executors.newFixedThreadPool(2, daemon("PocketCloud-TemplateServe"));
        watcher = new TemplateWatcher(jsonMeta, this::markDirty, this::markAllDirty,
                Path.of(PocketCloudPaths.TEMPLATES), Path.of(PocketCloudPaths.GROUPS));
        try {
            watcher.start();
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to watch the template folders; local changes are not replicated automatically", e);
        }
        running = true;
        cluster.scheduler().scheduleAtFixedRate(this::tickSafe, 1, 1, TimeUnit.SECONDS);
        CloudLogger.get().info("Template replication §aenabled §8(§b{} §rfolders watched§8)§r.", watcher.watchedFolders());
    }

    @Override
    public void stop() {
        running = false;
        if (watcher != null) watcher.stop();
        if (syncExecutor != null) syncExecutor.shutdownNow();
        if (serveExecutor != null) serveExecutor.shutdownNow();
    }

    private static ThreadFactory daemon(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        };
    }

    @Override
    public void onPeerConnected(String name) {
        if (name.equals(knownHead)) lastFullSync = 0;
    }

    // ------------------------------------------------------------------ scheduling

    private void tickSafe() {
        try {
            tick();
        } catch (Throwable t) {
            CloudLogger.get().exception("Template replication tick failed", t);
        }
    }

    private boolean ready() {
        return running && PocketCloud.instance().serverGroups().isLoaded();
    }

    private boolean isHead() {
        return cluster.isHead() && cluster.nodeName().equals(cluster.election().head());
    }

    private void tick() {
        if (!ready()) return;
        long now = System.currentTimeMillis();
        String head = cluster.election().head();
        if (!Objects.equals(head, knownHead)) {
            knownHead = head;
            headReady = false;
            lastFullSync = 0;
            if (head != null && head.equals(cluster.nodeName())) submit(this::initHead);
        }
        if (head == null) return;

        boolean self = head.equals(cluster.nodeName());
        if (!self && headConnected() && now - lastFullSync > RESYNC_MS) {
            lastFullSync = now;
            submit(this::fullSync);
        }

        pushPending.entrySet().removeIf(e -> now - e.getValue() > PUSH_TIMEOUT_MS);

        if (self && !headReady) return;
        for (Map.Entry<String, Dirty> entry : dirty.entrySet()) {
            Dirty d = entry.getValue();
            SyncUnit unit = new SyncUnit(entry.getKey());
            if (d.queued || paused(unit)) continue;
            if (now - d.lastEvent < DEBOUNCE_MS || now - d.checkedAt < RECHECK_MS) continue;
            d.queued = true;
            submit(() -> checkStable(unit, d));
        }
    }

    private void submit(ThrowingRunnable task) {
        if (!running) return;
        syncExecutor.execute(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                CloudLogger.get().exception("Template replication failed", t);
            }
        });
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private boolean headConnected() {
        String head = knownHead;
        return head != null && cluster.peer(head).map(Peer::connected).orElse(false);
    }

    void markDirty(SyncUnit unit) {
        if (unit.meta() && !jsonMeta) return;
        long now = System.currentTimeMillis();
        dirty.compute(unit.id(), (_, d) -> {
            if (d == null) d = new Dirty();
            d.lastEvent = now;
            return d;
        });
    }

    private void markAllDirty() {
        for (SyncUnit unit : localUnits()) markDirty(unit);
    }

    /** Runs on the sync thread once a unit had no events for 3 s: process it when its listing stays the same. */
    private void checkStable(SyncUnit unit, Dirty d) throws Exception {
        try {
            Map<String, UnitScanner.Stat> listing = scanner.listing(unit);
            long now = System.currentTimeMillis();
            boolean stable = d.listing != null || d.checkedAt > 0 ? Objects.equals(d.listing, listing) && d.lastEvent < d.checkedAt : false;
            if (!stable) {
                d.listing = listing;
                d.checkedAt = now;
                return;
            }
            dirty.remove(unit.id(), d);
        } finally {
            d.queued = false;
        }
        processLocal(unit);
    }

    // ------------------------------------------------------------------ units

    /** Units that exist on this node. */
    public List<SyncUnit> localUnits() {
        List<SyncUnit> units = new ArrayList<>();
        addDirs(units, Path.of(PocketCloudPaths.TEMPLATES), "templates/", "global");
        addDirs(units, Path.of(PocketCloudPaths.TEMPLATES_GLOBAL), "templates/global/", null);
        addDirs(units, Path.of(PocketCloudPaths.GROUPS), "groups/", null);
        if (jsonMeta && SyncUnit.META_FILES.values().stream().anyMatch(p -> Files.isRegularFile(Path.of(p)))) units.add(SyncUnit.META);
        return units;
    }

    private static void addDirs(List<SyncUnit> units, Path parent, String prefix, String skip) {
        if (!Files.isDirectory(parent)) return;
        try (var stream = Files.list(parent)) {
            for (Path dir : stream.sorted().toList()) {
                String name = dir.getFileName().toString();
                if (!Files.isDirectory(dir) || name.equals(skip) || !SyncUnit.validName(name)) continue;
                units.add(new SyncUnit(prefix + name));
            }
        } catch (IOException ignored) {
        }
    }

    private Path livePath(SyncUnit unit, String relative) {
        return unit.meta() ? Path.of(SyncUnit.META_FILES.get(relative)) : unit.root().resolve(relative);
    }

    private static String safeRelative(String path) {
        if (path == null || path.isEmpty() || path.startsWith("/") || path.contains("\\") || path.contains(":")) {
            throw new IllegalArgumentException("invalid path " + path);
        }
        for (String part : path.split("/")) {
            if (part.equals("..") || part.equals(".")) throw new IllegalArgumentException("invalid path " + path);
        }
        return path;
    }

    private static boolean validUnit(String id) {
        if (id == null) return false;
        if (id.equals(SyncUnit.META_ID)) return true;
        SyncUnit unit = SyncUnit.of(id, false);
        return unit != null && unit.id().equals(id);
    }

    public boolean paused(SyncUnit unit) {
        return pausedAll || paused.contains(unit.id());
    }

    // ------------------------------------------------------------------ packets

    @Override
    public boolean onPacket(String from, Channel channel, CloudPacket packet) {
        switch (packet) {
            case NodeTemplateListRequestPacket request -> cluster.respond(channel, request, listPacket());
            case HeadTemplateVersionPacket version -> {
                if (!from.equals(cluster.election().head()) || !validUnit(version.getUnit())) return true;
                submit(() -> onVersion(version));
            }
            case NodeTemplateManifestRequestPacket request -> serveExecutor.execute(() -> cluster.respond(channel, request, manifestPacket(request.getUnit())));
            case NodeTemplateChunkRequestPacket request -> serveExecutor.execute(() -> cluster.respond(channel, request, chunk(request)));
            case NodeTemplatePushPacket push -> {
                if (!isHead() || !headReady || !validUnit(push.getUnit())) {
                    cluster.respond(channel, push, new HeadTemplatePushAckPacket(false, "not ready"));
                    return true;
                }
                cluster.respond(channel, push, new HeadTemplatePushAckPacket(true, null));
                submit(() -> applyPush(from, push));
            }
            case NodeTemplateAdminPacket admin -> {
                if (!isHead() || !validUnit(admin.getUnit())) {
                    cluster.respond(channel, admin, new HeadTemplateAdminResultPacket(List.of("§cThis node is not the head (anymore).")));
                    return true;
                }
                SyncUnit unit = new SyncUnit(admin.getUnit());
                if (admin.getAction().equals("rollback")) {
                    cluster.respond(channel, admin, new HeadTemplateAdminResultPacket(List.of(
                            "Rolling back §b" + unit.display() + " §rto version §b" + admin.getVersion() + " §ron head §b" + cluster.nodeName() + "§r...")));
                    submit(() -> rollback(unit, admin.getVersion(), from));
                } else {
                    serveExecutor.execute(() -> cluster.respond(channel, admin, new HeadTemplateAdminResultPacket(historyLines(unit))));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private HeadTemplateListPacket listPacket() {
        List<HeadTemplateListPacket.Item> items = new ArrayList<>();
        if (isHead() && headReady) {
            headVersions.all().forEach((id, v) -> items.add(new HeadTemplateListPacket.Item(id, v.version(), v.hash())));
        }
        return new HeadTemplateListPacket(isHead() && headReady, items);
    }

    private HeadTemplateManifestPacket manifestPacket(String id) {
        if (!isHead() || !headReady || !validUnit(id)) return new HeadTemplateManifestPacket(false, 0, false, null);
        UnitVersion v = headVersions.get(new SyncUnit(id));
        if (v == null) return new HeadTemplateManifestPacket(false, 0, false, null);
        return new HeadTemplateManifestPacket(true, v.version(), v.deleted(), v.manifest().encode());
    }

    private NodeTemplateChunkPacket chunk(NodeTemplateChunkRequestPacket request) {
        try {
            if (!validUnit(request.getUnit())) throw new IllegalArgumentException("invalid unit");
            SyncUnit unit = new SyncUnit(request.getUnit());
            Path file = null;
            if (isHead() && request.getSha() != null && request.getSha().matches("[0-9a-f]{64}") && history.hasBlob(unit, request.getSha())) {
                file = history.blob(unit, request.getSha());
            }
            if (file == null) {
                String rel = safeRelative(request.getPath());
                if (unit.meta() && !SyncUnit.META_FILES.containsKey(rel)) throw new IllegalArgumentException("invalid meta file");
                file = livePath(unit, rel);
            }
            try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
                long size = ch.size();
                long offset = Math.max(0, request.getOffset());
                int length = (int) Math.max(0, Math.min(CHUNK_SIZE, size - offset));
                ByteBuffer buffer = ByteBuffer.allocate(length);
                while (buffer.hasRemaining()) {
                    if (ch.read(buffer, offset + buffer.position()) < 0) break;
                }
                return new NodeTemplateChunkPacket(true, null, size, Base64.getEncoder().encodeToString(Arrays.copyOf(buffer.array(), buffer.position())));
            }
        } catch (Exception e) {
            return new NodeTemplateChunkPacket(false, e.getClass().getSimpleName() + ": " + e.getMessage(), 0, null);
        }
    }

    private <T extends ResponsePacket> T request(String peer, RequestPacket request, Class<T> type) throws Exception {
        CompletableFuture<ResponsePacket> future = new CompletableFuture<>();
        request.then(future::complete).failure((_, ex, reason) ->
                future.completeExceptionally(ex != null ? ex : new IllegalStateException(String.valueOf(reason))));
        if (!cluster.send(peer, request)) throw new IllegalStateException("node " + peer + " is not connected");
        ResponsePacket response = future.get(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!type.isInstance(response)) throw new IllegalStateException("unexpected response " + response.getClass().getSimpleName());
        return type.cast(response);
    }

    /** Downloads a file with known hash from a node into {@code target} and verifies it. */
    private void fetch(String peer, SyncUnit unit, String path, String sha, Path target) throws Exception {
        Files.createDirectories(target.getParent());
        try (OutputStream out = Files.newOutputStream(target)) {
            long offset = 0;
            long total;
            do {
                NodeTemplateChunkPacket chunk = request(peer, new NodeTemplateChunkRequestPacket(unit.id(), path, sha, offset), NodeTemplateChunkPacket.class);
                if (!chunk.isOk()) throw new IOException(chunk.getError());
                byte[] bytes = Base64.getDecoder().decode(chunk.getData());
                total = chunk.getTotalSize();
                if (bytes.length == 0 && offset < total) throw new IOException("empty chunk");
                out.write(bytes);
                offset += bytes.length;
            } while (offset < total);
        }
        if (!UnitScanner.sha256(target).equals(sha)) throw new IOException("hash mismatch for " + path + " (file changed during the transfer)");
    }

    // ------------------------------------------------------------------ node side

    private void fullSync() throws Exception {
        String head = knownHead;
        if (!ready() || head == null || head.equals(cluster.nodeName()) || !headConnected()) return;
        HeadTemplateListPacket list = request(head, new NodeTemplateListRequestPacket(), HeadTemplateListPacket.class);
        if (!list.isReady()) {
            lastFullSync = System.currentTimeMillis() - RESYNC_MS + 3_000;
            return;
        }
        Set<String> known = new HashSet<>();
        for (HeadTemplateListPacket.Item item : list.getItems()) {
            if (!validUnit(item.unit()) || (!jsonMeta && item.unit().equals(SyncUnit.META_ID))) continue;
            known.add(item.unit());
            lastHeadVersions.put(item.unit(), item);
            reconcile(new SyncUnit(item.unit()), item.version(), item.hash());
        }
        for (SyncUnit unit : localUnits()) {
            // units the head does not know (created here while disconnected): send them via the normal path
            if (!known.contains(unit.id()) && !paused(unit) && !pushPending.containsKey(unit.id())) markDirty(unit);
        }
    }

    private void onVersion(HeadTemplateVersionPacket packet) throws Exception {
        if (packet.getOrigin().equals(cluster.nodeName())) pushPending.remove(packet.getUnit());
        lastHeadVersions.put(packet.getUnit(), new HeadTemplateListPacket.Item(packet.getUnit(), packet.getVersion(), packet.getHash()));
        if (!ready()) return; // the full sync after startup catches up
        if (packet.getUnit().equals(SyncUnit.META_ID) && !jsonMeta) return;
        reconcile(new SyncUnit(packet.getUnit()), packet.getVersion(), packet.getHash());
    }

    /** Brings a unit to the head's version: pushes unsent local changes first, otherwise pulls. */
    private void reconcile(SyncUnit unit, long version, String hash) throws Exception {
        if (!ready() || paused(unit) || pushPending.containsKey(unit.id()) || dirty.containsKey(unit.id()) || isHead()) return;
        UnitScanner.Scan scan = scanner.scan(unit);
        UnitVersion base = applied.get(unit);
        boolean synced = base != null && !base.deleted();

        if (DELETED.equals(hash)) {
            if (scan != null) {
                deleteLocal(unit);
                CloudLogger.get().info("Template §b{} §rwas deleted on the cluster and removed here.", unit.display());
            }
            if (base == null || !base.deleted() || base.version() != version) applied.put(unit, new UnitVersion(version, Manifest.EMPTY, true));
            return;
        }
        if (scan != null && scan.manifest().hash().equals(hash)) {
            pushedHashes.remove(unit.id());
            if (base == null || base.version() != version || !base.hash().equals(hash)) applied.put(unit, new UnitVersion(version, scan.manifest(), false));
            return;
        }
        String pushed = pushedHashes.get(unit.id());
        boolean unsent = scan == null ? synced && !DELETED.equals(pushed) : synced && !scan.manifest().hash().equals(base.hash()) && !scan.manifest().hash().equals(pushed);
        if (unsent) {
            // local changes the head does not have yet: send them first (after the usual debounce)
            markDirty(unit);
            return;
        }
        if (base == null && scan != null) {
            CloudLogger.get().warn("The local copy of §b{} §rdiffers from the cluster version; replacing it with version §b{}§r.", unit.display(), version);
        }
        pull(unit);
    }

    /** Local change detected by the watcher (after debounce + stability check). */
    private void processLocal(SyncUnit unit) throws Exception {
        if (!running) return;
        if (isHead()) {
            if (!headReady) {
                markDirty(unit);
                return;
            }
            acceptLocal(unit, cluster.nodeName(), null);
            return;
        }
        if (knownHead == null || !headConnected() || pushPending.containsKey(unit.id())) {
            markDirty(unit);
            return;
        }

        UnitScanner.Scan scan = scanner.scan(unit);
        UnitVersion base = applied.get(unit);
        boolean synced = base != null && !base.deleted();
        if (scan == null) {
            if (synced && !DELETED.equals(pushedHashes.get(unit.id()))) push(unit, null, base);
            return;
        }
        if (synced && (scan.manifest().hash().equals(base.hash()) || scan.manifest().hash().equals(pushedHashes.get(unit.id())))) {
            // nothing new (e.g. our own apply); catch up if the head moved on meanwhile
            HeadTemplateListPacket.Item known = lastHeadVersions.get(unit.id());
            if (known != null && !known.hash().equals(scan.manifest().hash())) reconcile(unit, known.version(), known.hash());
            return;
        }
        if (!synced) {
            // No version of the head taken over yet: if the head knows the unit, its version wins.
            HeadTemplateListPacket list = request(knownHead, new NodeTemplateListRequestPacket(), HeadTemplateListPacket.class);
            for (HeadTemplateListPacket.Item item : list.getItems()) {
                if (item.unit().equals(unit.id()) && !DELETED.equals(item.hash())) {
                    reconcile(unit, item.version(), item.hash());
                    return;
                }
            }
        }
        push(unit, scan, synced ? base : null);
    }

    private void push(SyncUnit unit, UnitScanner.Scan scan, UnitVersion base) throws Exception {
        String head = knownHead;
        if (head == null || head.equals(cluster.nodeName())) return;
        Manifest baseManifest = base == null || base.deleted() ? null : base.manifest();
        List<List<Object>> changes = new ArrayList<>();
        List<List<Object>> removed = new ArrayList<>();
        if (scan != null) {
            for (String path : Manifest.changed(baseManifest, scan.manifest())) {
                Manifest.Entry e = scan.manifest().get(path);
                Manifest.Entry old = baseManifest == null ? null : baseManifest.get(path);
                changes.add(List.of(path, e.size(), e.sha(), old == null ? "" : old.sha()));
            }
            for (String path : Manifest.removed(baseManifest, scan.manifest())) {
                removed.add(List.of(path, baseManifest.get(path).sha()));
            }
            if (changes.isEmpty() && removed.isEmpty() && base != null && !base.deleted()) return;
        }

        pushPending.put(unit.id(), System.currentTimeMillis());
        pushedHashes.put(unit.id(), scan == null ? DELETED : scan.manifest().hash());
        NodeTemplatePushPacket packet = new NodeTemplatePushPacket(unit.id(), base == null ? 0 : base.version(), scan == null,
                Manifest.compress(GSON.toJson(changes)), Manifest.compress(GSON.toJson(removed)), takeNote(unit));
        try {
            HeadTemplatePushAckPacket ack = request(head, packet, HeadTemplatePushAckPacket.class);
            if (!ack.isAccepted()) {
                pushPending.remove(unit.id());
                pushedHashes.remove(unit.id());
                markDirty(unit);
                CloudLogger.get().debug("Head did not take the changes of {}: {}", unit.display(), ack.getMessage());
                return;
            }
        } catch (Exception e) {
            pushPending.remove(unit.id());
            pushedHashes.remove(unit.id());
            markDirty(unit);
            throw e;
        }
        if (scan == null) {
            CloudLogger.get().info("Sent the deletion of §b{} §rto head §b{}§r.", unit.display(), head);
        } else {
            CloudLogger.get().info("Sent §b{} §rchanged and §b{} §rremoved file(s) of §b{} §rto head §b{}§r.",
                    changes.stream().filter(r -> !String.valueOf(r.getFirst()).endsWith("/")).count(),
                    removed.stream().filter(r -> !String.valueOf(r.getFirst()).endsWith("/")).count(), unit.display(), head);
        }
        // A templates.json/groups.json edited by hand on this node: the in-memory templates and groups follow it here too.
        if (unit.meta() && scan != null) MetaReloader.reload();
    }

    /** Fetches the head's version of a unit and swaps it in. */
    private void pull(SyncUnit unit) throws Exception {
        String head = knownHead;
        if (head == null || head.equals(cluster.nodeName())) return;
        HeadTemplateManifestPacket response = request(head, new NodeTemplateManifestRequestPacket(unit.id()), HeadTemplateManifestPacket.class);
        if (!response.isFound()) return;
        if (response.isDeleted()) {
            reconcile(unit, response.getVersion(), DELETED);
            return;
        }
        Manifest target = Manifest.decode(response.getManifest());
        for (String path : target.entries().keySet()) safeRelative(path);

        UnitScanner.Scan scan = scanner.scan(unit);
        Map<String, String> local = scan == null ? Map.of() : scan.manifest().pathsBySha();
        Path staging = directory.resolve("staging").resolve(unit.key() + "-" + System.nanoTime());
        Map<String, Path> fetched = new HashMap<>();
        long bytes = 0;
        try {
            for (Manifest.Entry e : target.entries().values()) {
                if (e.directory() || local.containsKey(e.sha()) || fetched.containsKey(e.sha())) continue;
                Path file = staging.resolve(e.sha());
                fetch(head, unit, e.path(), e.sha(), file);
                fetched.put(e.sha(), file);
                bytes += e.size();
            }
            boolean ok = applyToLive(unit, target, sha -> fetched.containsKey(sha) ? fetched.get(sha) : livePath(unit, local.get(sha)),
                    scan == null ? null : scan.listing());
            if (!ok) {
                CloudLogger.get().debug("{} changed locally while updating; retrying later", unit.display());
                markDirty(unit);
                return;
            }
        } finally {
            FileUtils.removeDirectory(staging);
        }
        applied.put(unit, new UnitVersion(response.getVersion(), target, false));
        pushedHashes.remove(unit.id());
        scanner.remember(unit, target);
        CloudLogger.get().info("Template §b{} §rupdated to version §b{} §8(§b{} §rfile(s) fetched, §b{} KB§8)§r.",
                unit.display(), response.getVersion(), fetched.size(), (bytes + 1023) / 1024);
        if (unit.meta()) MetaReloader.reload();
    }

    // ------------------------------------------------------------------ applying

    /**
     * Writes {@code target} into a temp folder and swaps it with the live folder under the template's write lock.
     * Ignored local files (logs, caches, ...) are carried over.
     *
     * @param source       content hash → file with that content
     * @param expected     listing the change was based on; if the live folder changed since, nothing is applied
     * @return false if the live folder changed in the meantime
     */
    private boolean applyToLive(SyncUnit unit, Manifest target, Function<String, Path> source, Map<String, UnitScanner.Stat> expected) throws IOException {
        if (unit.meta()) {
            if (!Objects.equals(scanner.listing(unit), expected)) return false;
            for (Manifest.Entry e : target.entries().values()) {
                Path live = livePath(unit, e.path());
                Path tmp = live.resolveSibling("." + live.getFileName() + ".pcsync");
                Files.copy(source.apply(e.sha()), tmp, StandardCopyOption.REPLACE_EXISTING);
                if (!UnitScanner.sha256(tmp).equals(e.sha())) {
                    Files.deleteIfExists(tmp);
                    throw new IOException("hash mismatch for " + e.path());
                }
                Files.move(tmp, live, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            return true;
        }

        Path root = unit.root();
        Path tmpBase = directory.resolve("tmp");
        Path tmp = tmpBase.resolve(unit.key() + "-" + System.nanoTime());
        Files.createDirectories(tmp);
        Path old = null;
        try {
            for (Manifest.Entry e : target.entries().values()) {
                Path dst = tmp.resolve(e.path());
                if (e.directory()) {
                    Files.createDirectories(dst);
                    continue;
                }
                Files.createDirectories(dst.getParent());
                Path src = source.apply(e.sha());
                if (src == null) throw new IOException("no content for " + e.path());
                if (src.startsWith(root)) {
                    try {
                        Files.createLink(dst, src);
                    } catch (IOException | UnsupportedOperationException ex) {
                        Files.copy(src, dst);
                    }
                } else {
                    Files.copy(src, dst);
                }
                if (Files.size(dst) != e.size()) throw new IOException("size mismatch for " + e.path());
            }
            if (Files.isDirectory(root)) carryOverIgnored(root, tmp);

            Lock lock = TemplateLocks.of(root).writeLock();
            lock.lock();
            try {
                if (!Objects.equals(scanner.listing(unit), expected)) return false;
                if (Files.exists(root)) {
                    old = tmpBase.resolve(unit.key() + "-old-" + System.nanoTime());
                    Files.move(root, old, StandardCopyOption.ATOMIC_MOVE);
                }
                Files.createDirectories(root.toAbsolutePath().getParent());
                Files.move(tmp, root, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                lock.unlock();
            }
            return true;
        } finally {
            if (Files.exists(tmp)) FileUtils.removeDirectory(tmp);
            if (old != null) FileUtils.removeDirectory(old);
        }
    }

    /** Files the ignore rules keep local (logs, log archive, caches) stay when a new version is swapped in. */
    private static void carryOverIgnored(Path root, Path tmp) throws IOException {
        SyncIgnore ignore = SyncIgnore.load(root);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            private boolean ignoredTree;
            private Path ignoredRoot;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.equals(root)) return FileVisitResult.CONTINUE;
                String rel = UnitScanner.relative(root, dir);
                if (ignoredRoot == null && ignore.ignored(rel, true)) ignoredRoot = dir;
                if (ignoredRoot != null) Files.createDirectories(tmp.resolve(rel));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                if (dir.equals(ignoredRoot)) ignoredRoot = null;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String rel = UnitScanner.relative(root, file);
                if (ignoredRoot == null && !ignore.ignored(rel, false)) return FileVisitResult.CONTINUE;
                Path dst = tmp.resolve(rel);
                if (Files.exists(dst)) return FileVisitResult.CONTINUE;
                Files.createDirectories(dst.getParent());
                try {
                    Files.createLink(dst, file);
                } catch (IOException | UnsupportedOperationException e) {
                    Files.copy(file, dst);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteLocal(SyncUnit unit) throws IOException {
        if (unit.meta()) return;
        Path root = unit.root();
        if (!Files.exists(root)) return;
        Path old = directory.resolve("tmp").resolve(unit.key() + "-old-" + System.nanoTime());
        Files.createDirectories(old.getParent());
        Lock lock = TemplateLocks.of(root).writeLock();
        lock.lock();
        try {
            Files.move(root, old, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            lock.unlock();
        }
        FileUtils.removeDirectory(old);
        scanner.forget(unit);
    }

    // ------------------------------------------------------------------ head side

    /** This node became head: determine the valid version of every unit from the local copy. */
    private void initHead() throws Exception {
        if (!isHead()) return;
        Set<String> ids = new TreeSet<>();
        for (SyncUnit unit : localUnits()) ids.add(unit.id());
        ids.addAll(headVersions.all().keySet());
        ids.addAll(applied.all().keySet());
        int changed = 0;
        for (String id : ids) {
            if (!validUnit(id) || (!jsonMeta && id.equals(SyncUnit.META_ID))) continue;
            SyncUnit unit = new SyncUnit(id);
            UnitVersion prev = headVersions.get(unit);
            UnitVersion app = applied.get(unit);
            long base = Math.max(prev == null ? 0 : prev.version(), app == null ? 0 : app.version());
            UnitScanner.Scan scan = scanner.scan(unit);
            if (scan == null) {
                boolean known = (prev != null && !prev.deleted()) || (app != null && !app.deleted());
                if (known) {
                    UnitVersion tomb = new UnitVersion(base + 1, Manifest.EMPTY, true);
                    record(unit, tomb, prev == null ? (app == null ? null : app.manifest()) : prev.manifest(), cluster.nodeName(), "deleted", List.of());
                    headVersions.put(unit, tomb);
                    changed++;
                } else if (prev == null && app != null) {
                    headVersions.put(unit, app);
                }
                continue;
            }
            String hash = scan.manifest().hash();
            boolean same = (prev != null && prev.hash().equals(hash)) || (app != null && app.hash().equals(hash));
            if (!history.ensureBlobs(unit, scan.manifest(), path -> livePath(unit, path))) {
                markDirty(unit);
            }
            long version = same ? Math.max(base, 1) : base + 1;
            UnitVersion now = new UnitVersion(version, scan.manifest(), false);
            boolean hasHistory = history.record(unit, version).isPresent();
            if (!same || !hasHistory) {
                record(unit, now, prev == null ? (app == null ? null : app.manifest()) : prev.manifest(), cluster.nodeName(),
                        same ? "baseline" : (prev == null && app == null ? "initial version" : "changed while offline"), List.of());
                if (!same) changed++;
            }
            headVersions.put(unit, now);
        }
        headReady = true;
        CloudLogger.get().info("Template replication: this node is §6head §rand serves §b{} §runit(s) §8(§b{} §rnew version(s)§8)§r.",
                headVersions.all().size(), changed);
        for (Map.Entry<String, UnitVersion> e : headVersions.all().entrySet()) broadcastVersion(new SyncUnit(e.getKey()), null);
    }

    private void record(SyncUnit unit, UnitVersion version, Manifest previous, String node, String note, List<String> conflicts) throws IOException {
        TemplateHistory.Record r = new TemplateHistory.Record();
        r.version = version.version();
        r.time = System.currentTimeMillis();
        r.node = node;
        r.note = note;
        r.deleted = version.deleted();
        r.changed = new ArrayList<>(Manifest.changed(previous, version.manifest()));
        r.removed = new ArrayList<>(Manifest.removed(previous, version.manifest()));
        r.conflicts = new ArrayList<>(conflicts);
        r.manifest = version.manifest().toList();
        history.add(unit, r);
    }

    private void broadcastVersion(SyncUnit unit, String origin) {
        UnitVersion v = headVersions.get(unit);
        if (v == null) return;
        cluster.broadcast(() -> new HeadTemplateVersionPacket(unit.id(), v.version(), v.hash(), origin));
    }

    /** Head: takes over what changed in its own folder (watcher, or before merging a push). */
    private void acceptLocal(SyncUnit unit, String node, String origin) throws Exception {
        UnitScanner.Scan scan = scanner.scan(unit);
        UnitVersion current = headVersions.get(unit);
        if (scan == null) {
            if (current == null || current.deleted()) return;
            UnitVersion tomb = new UnitVersion(current.version() + 1, Manifest.EMPTY, true);
            record(unit, tomb, current.manifest(), node, "deleted", List.of());
            headVersions.put(unit, tomb);
            CloudLogger.get().info("Template §b{} §rwas deleted §8(§rversion §b{}§8)§r, removing it on all nodes.", unit.display(), tomb.version());
            broadcastVersion(unit, origin);
            return;
        }
        if (current != null && !current.deleted() && current.hash().equals(scan.manifest().hash())) return;
        if (!history.ensureBlobs(unit, scan.manifest(), path -> livePath(unit, path))) {
            markDirty(unit); // still being written
            return;
        }
        Manifest previous = current == null ? null : current.manifest();
        UnitVersion now = new UnitVersion(current == null ? 1 : current.version() + 1, scan.manifest(), false);
        String note = node.equals(cluster.nodeName()) ? takeNote(unit) : "";
        record(unit, now, previous, node, "changed on " + node + (note.isEmpty() ? "" : " (" + note + ")"), List.of());
        headVersions.put(unit, now);
        CloudLogger.get().info("Template §b{} §ris now version §b{} §8(§rchanged on §b{}§r: §b{} §rchanged, §b{} §rremoved§8)§r.",
                unit.display(), now.version(), node, files(Manifest.changed(previous, now.manifest())), files(Manifest.removed(previous, now.manifest())));
        broadcastVersion(unit, origin);
    }

    /** Head: merges a node's delta into the valid version. */
    private void applyPush(String from, NodeTemplatePushPacket push) throws Exception {
        SyncUnit unit = new SyncUnit(push.getUnit());
        if (!isHead()) return;
        acceptLocal(unit, cluster.nodeName(), null);
        UnitVersion current = headVersions.get(unit);
        boolean exists = current != null && !current.deleted();

        if (push.isUnitDeleted()) {
            if (!exists) {
                sendVersion(from, unit);
                return;
            }
            deleteLocal(unit);
            UnitVersion tomb = new UnitVersion(current.version() + 1, Manifest.EMPTY, true);
            record(unit, tomb, current.manifest(), from, "deleted on " + from, List.of());
            headVersions.put(unit, tomb);
            CloudLogger.get().info("Template §b{} §rwas deleted on §b{} §8(§rversion §b{}§8)§r, removing it on all nodes.", unit.display(), from, tomb.version());
            broadcastVersion(unit, from);
            return;
        }

        List<List<Object>> changes = GSON.fromJson(Manifest.decompress(push.getChanges()), new TypeToken<List<List<Object>>>() {}.getType());
        List<List<Object>> removed = GSON.fromJson(Manifest.decompress(push.getRemoved()), new TypeToken<List<List<Object>>>() {}.getType());
        Manifest base = exists ? current.manifest() : Manifest.EMPTY;
        SortedMap<String, Manifest.Entry> merged = new TreeMap<>(base.entries());
        List<String> conflicts = new ArrayList<>();
        List<Manifest.Entry> incoming = new ArrayList<>();
        for (List<Object> row : changes) {
            String path = safeRelative(String.valueOf(row.get(0)));
            if (unit.meta() && !SyncUnit.META_FILES.containsKey(path)) continue;
            Manifest.Entry entry = new Manifest.Entry(path, ((Number) row.get(1)).longValue(), String.valueOf(row.get(2)));
            String baseSha = String.valueOf(row.get(3));
            Manifest.Entry cur = merged.get(path);
            if (cur != null && !cur.sha().equals(baseSha) && !cur.sha().equals(entry.sha())) conflicts.add(path);
            merged.put(path, entry);
            if (!entry.directory()) incoming.add(entry);
        }
        for (List<Object> row : removed) {
            String path = safeRelative(String.valueOf(row.get(0)));
            Manifest.Entry cur = merged.get(path);
            if (cur == null) continue;
            if (!cur.sha().equals(String.valueOf(row.get(1)))) conflicts.add(path);
            merged.remove(path);
        }
        addParentDirs(merged);
        Manifest target = new Manifest(merged);
        if (exists && target.hash().equals(current.hash())) {
            sendVersion(from, unit);
            return;
        }

        Path staging = directory.resolve("staging").resolve("push-" + unit.key() + "-" + System.nanoTime());
        try {
            for (Manifest.Entry e : incoming) {
                if (history.hasBlob(unit, e.sha())) continue;
                Path file = staging.resolve(e.sha());
                fetch(from, unit, e.path(), e.sha(), file);
                history.addBlob(unit, e.sha(), file);
            }
        } catch (Exception e) {
            CloudLogger.get().warn("Could not take over the changes of §b{} §rfrom §b{}§r: {}", unit.display(), from, e.getMessage());
            sendVersion(from, unit);
            return;
        } finally {
            FileUtils.removeDirectory(staging);
        }

        // make sure every file of the new version is available as blob (unchanged files come from the old version)
        for (Manifest.Entry e : target.entries().values()) {
            if (!e.directory() && !history.hasBlob(unit, e.sha())) {
                history.ensureBlobs(unit, new Manifest(new TreeMap<>(Map.of(e.path(), e))), path -> livePath(unit, path));
            }
        }

        UnitScanner.Scan live = scanner.scan(unit);
        if (!applyToLive(unit, target, sha -> history.blob(unit, sha), live == null ? null : live.listing())) {
            CloudLogger.get().debug("{} changed on the head while merging; retrying", unit.display());
            acceptLocal(unit, cluster.nodeName(), null);
            submit(() -> applyPush(from, push));
            return;
        }
        scanner.remember(unit, target);

        String lastNode = null;
        for (String path : conflicts) {
            lastNode = lastChanger(unit, path);
            CloudLogger.get().warn("§cConflict §rin §b{}§8/§b{}§r: changed on §b{} §rand on §b{} §rat the same time. The later change from §b{} §rwins, the other version stays in the history §8(§rversion §b{}§8)§r.",
                    unit.display(), path, from, lastNode == null ? "another node" : lastNode, from, current == null ? 0 : current.version());
        }
        UnitVersion now = new UnitVersion(current == null ? 1 : current.version() + 1, target, false);
        String note = push.getNote() == null || push.getNote().isEmpty() ? "" : " (" + push.getNote() + ")";
        record(unit, now, exists ? current.manifest() : null, from, (conflicts.isEmpty() ? "changed on " + from : "changed on " + from + " (conflict)") + note, conflicts);
        headVersions.put(unit, now);
        CloudLogger.get().info("Template §b{} §ris now version §b{} §8(§rchanged on §b{}§r: §b{} §rchanged, §b{} §rremoved§8)§r.",
                unit.display(), now.version(), from, files(Manifest.changed(exists ? current.manifest() : null, target)),
                files(Manifest.removed(exists ? current.manifest() : null, target)));
        if (unit.meta()) MetaReloader.reload();
        broadcastVersion(unit, from);
    }

    private static long files(List<String> paths) {
        return paths.stream().filter(p -> !p.endsWith("/")).count();
    }

    private static void addParentDirs(SortedMap<String, Manifest.Entry> entries) {
        for (String path : List.copyOf(entries.keySet())) {
            String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            int idx;
            while ((idx = p.lastIndexOf('/')) > 0) {
                p = p.substring(0, idx);
                entries.putIfAbsent(p + "/", new Manifest.Entry(p + "/", 0, Manifest.DIR));
            }
        }
    }

    private String lastChanger(SyncUnit unit, String path) {
        List<TemplateHistory.Record> records = history.records(unit);
        for (int i = records.size() - 1; i >= 0; i--) {
            TemplateHistory.Record r = records.get(i);
            if (r.changed.contains(path) || r.removed.contains(path)) return r.node;
        }
        return null;
    }

    private void sendVersion(String peer, SyncUnit unit) {
        UnitVersion v = headVersions.get(unit);
        if (v == null) {
            cluster.send(peer, new HeadTemplateVersionPacket(unit.id(), 0, DELETED, peer));
            return;
        }
        cluster.send(peer, new HeadTemplateVersionPacket(unit.id(), v.version(), v.hash(), peer));
    }

    /** Head: restores a version from the history as a new version and distributes it. */
    private void rollback(SyncUnit unit, long version, String requestedBy) throws Exception {
        if (!isHead()) return;
        TemplateHistory.Record record = history.record(unit, version).orElse(null);
        if (record == null) {
            CloudLogger.get().error("Version §b{} §rof §b{} §ris not in the history.", version, unit.display());
            return;
        }
        acceptLocal(unit, cluster.nodeName(), null);
        UnitVersion current = headVersions.get(unit);
        long next = current == null ? version + 1 : current.version() + 1;
        Manifest previous = current == null || current.deleted() ? null : current.manifest();
        if (record.deleted) {
            deleteLocal(unit);
            UnitVersion tomb = new UnitVersion(next, Manifest.EMPTY, true);
            record(unit, tomb, previous, requestedBy, "rollback to version " + version, List.of());
            headVersions.put(unit, tomb);
        } else {
            Manifest target = record.manifest();
            for (Manifest.Entry e : target.entries().values()) {
                if (!e.directory() && !history.hasBlob(unit, e.sha())) {
                    CloudLogger.get().error("Cannot roll back §b{}§r: the content of §b{} §ris missing in the history.", unit.display(), e.path());
                    return;
                }
            }
            UnitScanner.Scan live = scanner.scan(unit);
            if (!applyToLive(unit, target, sha -> history.blob(unit, sha), live == null ? null : live.listing())) {
                CloudLogger.get().error("§b{} §rchanged during the rollback, please try again.", unit.display());
                return;
            }
            scanner.remember(unit, target);
            UnitVersion now = new UnitVersion(next, target, false);
            record(unit, now, previous, requestedBy, "rollback to version " + version, List.of());
            headVersions.put(unit, now);
            if (unit.meta()) MetaReloader.reload();
        }
        CloudLogger.get().success("Rolled back §b{} §rto version §b{} §8(§rnow version §b{}§8)§r, distributing it to all nodes.", unit.display(), version, next);
        broadcastVersion(unit, null);
    }

    // ------------------------------------------------------------------ console

    public List<String> historyLines(SyncUnit unit) {
        List<String> lines = new ArrayList<>();
        List<TemplateHistory.Record> records = history.records(unit);
        UnitVersion current = headVersions.get(unit);
        if (records.isEmpty()) {
            lines.add("§cNo history for §b" + unit.display() + "§c.");
            return lines;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        lines.add("History of §b" + unit.display() + " §8(§rcurrent version§8: §b" + (current == null ? "-" : current.version()) + "§8, §rhead§8: §b" + cluster.nodeName() + "§8)§r:");
        for (int i = records.size() - 1; i >= 0; i--) {
            TemplateHistory.Record r = records.get(i);
            List<String> files = new ArrayList<>();
            r.changed.stream().filter(p -> !p.endsWith("/")).limit(5).forEach(p -> files.add("§a~" + p));
            r.removed.stream().filter(p -> !p.endsWith("/")).limit(5).forEach(p -> files.add("§c-" + p));
            lines.add("§bv" + r.version + " §8| §r" + format.format(new Date(r.time)) + " §8| §rnode §b" + r.node + " §8| §r" + r.note
                    + (r.deleted ? " §8| §cdeleted" :  " §8| §b" + files(r.changed) + " §rchanged, §b" + files(r.removed) + " §rremoved")
                    + (r.conflicts.isEmpty() ? "" : " §8| §cconflicts§8: §r" + String.join(", ", r.conflicts))
                    + (files.isEmpty() ? "" : " §8| " + String.join("§8, ", files)));
        }
        return lines;
    }

    /** {@code template history}: local on the head, forwarded otherwise. */
    public void history(SyncUnit unit, java.util.function.Consumer<List<String>> output) {
        if (isHead()) {
            output.accept(historyLines(unit));
            return;
        }
        forward(new NodeTemplateAdminPacket("history", unit.id(), 0), output);
    }

    /** {@code template rollback}: runs on the head. */
    public void rollback(SyncUnit unit, long version, java.util.function.Consumer<List<String>> output) {
        if (isHead()) {
            output.accept(List.of("Rolling back §b" + unit.display() + " §rto version §b" + version + "§r..."));
            submit(() -> rollback(unit, version, cluster.nodeName()));
            return;
        }
        forward(new NodeTemplateAdminPacket("rollback", unit.id(), version), output);
    }

    private void forward(NodeTemplateAdminPacket packet, java.util.function.Consumer<List<String>> output) {
        String head = knownHead;
        if (head == null || !headConnected()) {
            output.accept(List.of("§cThere is no head right now."));
            return;
        }
        serveExecutor.execute(() -> {
            try {
                output.accept(request(head, packet, HeadTemplateAdminResultPacket.class).getLines());
            } catch (Exception e) {
                output.accept(List.of("§cThe head did not answer: " + e.getMessage()));
            }
        });
    }

    public void pause(SyncUnit unit) {
        if (unit == null) pausedAll = true;
        else paused.add(unit.id());
    }

    public void resume(SyncUnit unit) {
        if (unit == null) {
            pausedAll = false;
            paused.clear();
        } else {
            paused.remove(unit.id());
        }
        // changes made while paused are picked up by a fresh scan; the head's versions by a full sync
        for (SyncUnit u : localUnits()) {
            if (unit == null || u.equals(unit)) markDirty(u);
        }
        if (unit != null) markDirty(unit);
        lastFullSync = 0;
    }

    // ------------------------------------------------------------------ versions for server starts

    /**
     * A server of the template is saved into the template on this node: the next version of the template
     * is tagged with it in the history (e.g. "changed on node-2 (save of Lobby-1)").
     */
    public void noteSave(String template, String serverName) {
        notes.put("templates/" + template, Map.entry("save of " + serverName, System.currentTimeMillis()));
    }

    private String takeNote(SyncUnit unit) {
        Map.Entry<String, Long> note = notes.remove(unit.id());
        return note == null || System.currentTimeMillis() - note.getValue() > 60_000 ? "" : note.getKey();
    }


    /** Units a server of the template is built from: the template, the global folder of its type and its groups. */
    public static List<SyncUnit> unitsOf(de.pocketcloud.api.component.template.ITemplate template) {
        List<SyncUnit> units = new ArrayList<>();
        units.add(new SyncUnit("templates/" + template.name()));
        units.add(new SyncUnit("templates/global/" + template.templateType().name().toLowerCase()));
        for (var group : template.parentGroups()) {
            if (SyncUnit.validName(group.name())) units.add(new SyncUnit("groups/" + group.name()));
        }
        return units;
    }

    /**
     * Head: the valid versions ({@code unit → "version:hash"}) a node must have before it starts a server of the
     * template. Changes made on the head that are still waiting for the watcher's debounce are taken over first,
     * so a start right after an edit uses the edited files. Resolves with an empty map if this node does not
     * (yet) serve versions.
     */
    public CompletableFuture<Map<String, Object>> expectedVersions(de.pocketcloud.api.component.template.ITemplate template) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        if (!running || !isHead() || !headReady) {
            future.complete(Map.of());
            return future;
        }
        List<SyncUnit> units = unitsOf(template);
        submit(() -> {
            try {
                Map<String, Object> versions = new LinkedHashMap<>();
                for (SyncUnit unit : units) {
                    if (isHead() && headReady && !paused(unit) && dirty.remove(unit.id()) != null) acceptLocal(unit, cluster.nodeName(), null);
                    UnitVersion v = headVersions.get(unit);
                    if (v != null && !v.deleted()) versions.put(unit.id(), v.version() + ":" + v.hash());
                }
                future.complete(versions);
            } catch (Exception e) {
                future.complete(Map.of());
                throw e;
            }
        });
        // replication shut down or stuck meanwhile: start without the check
        return future.completeOnTimeout(Map.of(), 3, TimeUnit.SECONDS);
    }

    /**
     * Node: makes sure the local copies have the versions the head expects ({@link #expectedVersions}) before a
     * server is prepared, fetching them from the head if not.
     *
     * @return true if something had to be synced; fails if a unit could not be brought to the expected version
     * within the timeout or has local changes the head does not have yet
     */
    public CompletableFuture<Boolean> ensureVersions(Map<String, Object> expected, String serverName, long timeoutMs) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (expected == null || expected.isEmpty() || !running || isHead()) {
            future.complete(false);
            return future;
        }
        // Versions this node has not taken over yet when the request arrives (an update may already be queued).
        Set<String> behind = new HashSet<>();
        for (Map.Entry<String, Object> e : expected.entrySet()) {
            String value = String.valueOf(e.getValue());
            int colon = value.indexOf(':');
            if (!validUnit(e.getKey()) || colon < 0) continue;
            UnitVersion have = applied.get(new SyncUnit(e.getKey()));
            if (have == null || !have.hash().equals(value.substring(colon + 1))) {
                behind.add(e.getKey());
                CloudLogger.get().info("The head expects version §b{} §rof §b{} §rfor §b{}§r, this node has §b{}§r: making sure it is synced first...",
                        value.substring(0, colon), new SyncUnit(e.getKey()).display(), serverName, have == null ? "-" : have.version());
            }
        }
        try {
            syncExecutor.execute(() -> {
                try {
                    boolean synced = false;
                    for (Map.Entry<String, Object> e : expected.entrySet()) {
                        String value = String.valueOf(e.getValue());
                        int colon = value.indexOf(':');
                        if (!validUnit(e.getKey()) || colon < 0) continue;
                        SyncUnit unit = new SyncUnit(e.getKey());
                        boolean fetched = ensureVersion(unit, Long.parseLong(value.substring(0, colon)), value.substring(colon + 1), serverName);
                        if (!fetched && behind.contains(e.getKey())) {
                            UnitVersion v = applied.get(unit);
                            CloudLogger.get().success("Template §b{} §ris synced §8(§rversion §b{}§8)§r, starting §b{}§r.", unit.display(), v == null ? "-" : v.version(), serverName);
                        }
                        synced |= fetched || behind.contains(e.getKey());
                    }
                    future.complete(synced);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (RejectedExecutionException e) {
            future.completeExceptionally(new IllegalStateException("template replication is shutting down"));
        }
        return future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /** Runs on the sync thread. Returns true if the unit was fetched. */
    private boolean ensureVersion(SyncUnit unit, long version, String hash, String serverName) throws Exception {
        if (paused(unit)) {
            CloudLogger.get().warn("Sync of §b{} §ris paused: starting §b{} §rwith the local copy.", unit.display(), serverName);
            return false;
        }
        UnitScanner.Scan scan = scanner.scan(unit);
        String local = scan == null ? null : scan.manifest().hash();
        if (hash.equals(local)) return false;

        UnitVersion base = applied.get(unit);
        boolean synced = base != null && !base.deleted();
        if (synced && local != null && !local.equals(base.hash()) && !local.equals(pushedHashes.get(unit.id()))) {
            throw new IllegalStateException("the template " + unit.display() + " has local changes on " + cluster.nodeName() + " that are not replicated yet");
        }
        CloudLogger.get().info("Template §b{} §ris at version §b{} §rhere, the head expects §b{}§r: syncing it before starting §b{}§r...",
                unit.display(), base == null ? "-" : base.version(), version, serverName);
        pull(unit);
        UnitScanner.Scan after = scanner.scan(unit);
        String now = after == null ? null : after.manifest().hash();
        UnitVersion v = applied.get(unit);
        // the head may have moved on to an even newer version meanwhile: that one is fine too
        boolean newer = v != null && !v.deleted() && v.version() >= version && v.hash().equals(now);
        if (!hash.equals(now) && !newer) {
            throw new IllegalStateException("could not sync the template " + unit.display() + " to version " + version);
        }
        CloudLogger.get().success("Template §b{} §rsynced to version §b{} §rbefore starting §b{}§r.", unit.display(), v == null ? version : v.version(), serverName);
        return true;
    }

    public List<String> statusLines() {
        List<String> lines = new ArrayList<>();
        boolean head = isHead();
        lines.add("Template replication §8| §rthis node§8: §b" + cluster.nodeName() + (head ? " §6[HEAD]" : "") + " §8| §rhead§8: §b"
                + (knownHead == null ? "§cnone" : knownHead) + " §8| §rwatched folders§8: §b" + watcher.watchedFolders()
                + (pausedAll ? " §8| §cpaused (all)" : ""));
        Set<String> ids = new TreeSet<>();
        localUnits().forEach(u -> ids.add(u.id()));
        ids.addAll((head ? headVersions : applied).all().keySet());
        for (String id : ids) {
            SyncUnit unit = new SyncUnit(id);
            UnitVersion v = (head ? headVersions : applied).get(unit);
            HeadTemplateListPacket.Item h = lastHeadVersions.get(id);
            String state;
            if (paused(unit)) state = "§cpaused";
            else if (pushPending.containsKey(id)) state = "§esending";
            else if (dirty.containsKey(id)) state = "§echanged";
            else if (v != null && v.deleted()) state = "§7deleted";
            else if (head || h == null || v == null) state = v == null ? "§eunknown" : "§aok";
            else state = h.hash().equals(v.hash()) ? "§ain sync" : "§eoutdated §8(§rhead v" + h.version() + "§8)";
            lines.add("§b" + unit.display() + " §8| §rversion §b" + (v == null ? "-" : v.version()) + " §8| §r" + (v == null ? "-" : Manifest.shortHash(v.hash()))
                    + " §8| §r" + (v == null || v.deleted() ? 0 : v.manifest().fileCount()) + " file(s) §8| " + state);
        }
        return lines;
    }
}
