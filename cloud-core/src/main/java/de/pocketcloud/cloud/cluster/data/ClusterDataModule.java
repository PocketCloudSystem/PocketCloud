package de.pocketcloud.cloud.cluster.data;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.template.util.TemplateEditData;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cache.NotificationListCache;
import de.pocketcloud.cloud.cache.WhitelistCache;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.ClusterModule;
import de.pocketcloud.cloud.cluster.Peer;
import de.pocketcloud.cloud.cluster.data.packet.*;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.provider.CloudJsonProvider;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.TemplateManager;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.template.group.ServerGroupManager;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.shared.event.group.ServerGroupCreatedEvent;
import de.pocketcloud.shared.event.group.ServerGroupDeletedEvent;
import de.pocketcloud.shared.event.group.ServerGroupUpdatedEvent;
import de.pocketcloud.shared.event.template.TemplateCreatedEvent;
import de.pocketcloud.shared.event.template.TemplateDeletedEvent;
import de.pocketcloud.shared.event.template.TemplateEditedEvent;
import io.netty.channel.Channel;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Cluster-wide provider data: whitelist, notification list, templates and groups.
 * <ul>
 *     <li>Writes on a node that is not head are sent to the head ({@link NodeDataWritePacket}): whitelist and
 *     notifications by {@link ClusterDataProvider}, templates and groups by the managers ({@link #forwardToHead}).</li>
 *     <li>The head writes the provider, then Redis (if enabled), then tells all nodes ({@link HeadDataChangedPacket}).</li>
 *     <li>The nodes update their in-memory state. With the JSON provider they also write their own
 *     {@code whitelist.json}/{@code notificationList.json} (not replicated as files), so a later head has them.
 *     {@code templates.json}/{@code groups.json} are replicated as the {@code meta} unit by the template replication
 *     and are not written here. With MySQL all nodes share the database, only the caches are updated.</li>
 *     <li>After a head change or a reconnect a node asks the head for a snapshot of what it may have missed.</li>
 *     <li>A node that becomes head loads the provider data into Redis.</li>
 * </ul>
 */
public final class ClusterDataModule implements ClusterModule {

    static final String KIND_WHITELIST = "whitelist";
    static final String KIND_NOTIFICATIONS = "notifications";
    static final String KIND_TEMPLATE = "template";
    static final String KIND_GROUP = "group";

    private static final String OP_TEMPLATE_CREATE = "template.create";
    private static final String OP_TEMPLATE_EDIT = "template.edit";
    private static final String OP_TEMPLATE_DELETE = "template.delete";
    private static final String OP_GROUP_CREATE = "group.create";
    private static final String OP_GROUP_ADD_TEMPLATE = "group.addTemplate";
    private static final String OP_GROUP_REMOVE_TEMPLATE = "group.removeTemplate";
    private static final String OP_GROUP_DELETE = "group.delete";

    /** A replicated templates.json does not undo a change the head announced this recently (it may be older). */
    private static final long ANNOUNCED_WINS_MS = 20_000;

    private static final Type DOCUMENT = new TypeToken<Map<String, Object>>() {}.getType();
    private static final Type SNAPSHOT = new TypeToken<Map<String, Object>>() {}.getType();

    private ClusterManager cluster;
    private ExecutorService executor;
    private volatile boolean running;
    private volatile String knownHead;
    private volatile boolean wasHead;
    private volatile String snapshotFrom;
    private volatile long snapshotRequested;
    /** template name → time a head announcement for it was applied here. */
    private final Map<String, Long> announcedTemplates = new ConcurrentHashMap<>();

    public static Optional<ClusterDataModule> get() {
        ClusterManager cluster = PocketCloud.instance().cluster();
        if (!cluster.clustered()) return Optional.empty();
        for (ClusterModule module : cluster.modules()) {
            if (module instanceof ClusterDataModule m && m.running) return Optional.of(m);
        }
        return Optional.empty();
    }

    @Override
    public Collection<Class<? extends Packet>> packets() {
        return List.of(NodeDataWritePacket.class, HeadDataWriteResultPacket.class, HeadDataChangedPacket.class,
                NodeDataSnapshotRequestPacket.class, HeadDataSnapshotPacket.class);
    }

    @Override
    public void start(ClusterManager cluster) {
        this.cluster = cluster;
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PocketCloud-ClusterData");
            t.setDaemon(true);
            return t;
        });
        running = true;
        cluster.scheduler().scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        running = false;
        if (executor != null) executor.shutdownNow();
    }

    boolean isHead() {
        return cluster.isHead();
    }

    private void execute(Runnable task) {
        if (!running) return;
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } catch (Throwable t) {
                    CloudLogger.get().exception("Cluster data task failed", t);
                }
            });
        } catch (RuntimeException ignored) {
            // shutting down
        }
    }

    // ------------------------------------------------------------------ head changes

    private void tick() {
        if (!running) return;
        String head = cluster.election().head();
        boolean head0 = cluster.isHead();
        if (!Objects.equals(head, knownHead)) {
            knownHead = head;
            snapshotFrom = null;
        }
        if (head0 && !wasHead) {
            // This node became head: Redis gets the provider data (spec: the head loads everything from the provider).
            execute(() -> CloudProvider.redis().ifPresent(redis -> {
                redis.fillFromProvider();
                CloudLogger.get().info("Loaded the whitelist, notifications, templates and groups into §bRedis§r.");
            }));
        }
        wasHead = head0;
        if (head0 || head == null || !PocketCloud.instance().serverGroups().isLoaded()) return;

        boolean connected = cluster.peer(head).map(Peer::connected).orElse(false);
        long now = System.currentTimeMillis();
        if (connected && !head.equals(snapshotFrom) && now - snapshotRequested > 10_000) {
            snapshotRequested = now;
            requestSnapshot(head);
        }
    }

    @Override
    public void onPeerConnected(String name) {
        if (name.equals(knownHead)) {
            snapshotFrom = null;
            snapshotRequested = 0;
        }
    }

    @Override
    public void onPeerGone(String name) {
        if (name.equals(knownHead)) snapshotFrom = null;
    }

    // ------------------------------------------------------------------ forwarding (non-head → head)

    /**
     * Sends a template/group write to the head if this node is a cluster member that is not head.
     *
     * @param what for the log, e.g. "the edit of template Lobby"
     * @return true if the write was forwarded (the caller must not execute it locally)
     */
    public static boolean forwardToHead(String operation, String target, Object payload, String what) {
        Optional<ClusterDataModule> module = get().filter(m -> !m.isHead());
        if (module.isEmpty()) return false;
        String head = module.get().cluster.election().head();
        CloudLogger.get().info("Forwarding {} to head §b{}§r...", what, head == null ? "?" : head);
        module.get().forward(operation, target, payload)
                .thenSuccess(message -> CloudLogger.get().success("Head §b{} §rapplied {}{}", head, what, message.isEmpty() ? "." : "§8: §r" + message))
                .failure(ex -> CloudLogger.get().error("§cCould not apply {} on the head§r: {}", what, ex.getMessage()));
        return true;
    }

    /** Sends a write to the head. Resolves with the head's message once it was applied there. */
    Promise<String> forward(String operation, String target, Object payload) {
        String head = cluster.election().head();
        if (head == null || head.isEmpty()) return Promise.rejected(new IllegalStateException("there is no head right now"));
        String json = payload == null ? "" : payload instanceof String s ? s : FileUtils.GSON.toJson(payload);
        Promise<String> promise = new Promise<>();
        NodeDataWritePacket request = new NodeDataWritePacket(operation, target, json);
        request.then(response -> {
            if (response instanceof HeadDataWriteResultPacket r && r.isSuccess()) promise.resolve(r.getMessage());
            else if (response instanceof HeadDataWriteResultPacket r) promise.reject(new IllegalArgumentException(r.getMessage()));
            else promise.reject(new IllegalStateException("unexpected response"));
        }).failure((_, ex, reason) -> promise.reject(ex != null ? ex : new IllegalStateException(String.valueOf(reason))));
        if (!cluster.send(head, request)) return Promise.rejected(new IllegalStateException("the head " + head + " is not connected"));
        CloudLogger.get().debug("Forwarded {} {} to head {}", operation, target, head);
        return promise;
    }

    // ------------------------------------------------------------------ announcing (head → all)

    /** Head: tells every other node about a write that is now in the provider (and Redis). */
    void announce(String kind, String key, Object document) {
        String encoded = document == null ? "" : document instanceof Boolean b ? String.valueOf(b) : FileUtils.GSON.toJson(document);
        cluster.broadcast(() -> new HeadDataChangedPacket(kind, key, encoded));
        CloudLogger.get().debug("Announced {} {} to all nodes", kind, key);
    }

    /** Whether a head announcement for this template was applied here just now (see {@code MetaReloader}). */
    public static boolean announcedRecently(String template) {
        return get().map(m -> {
            Long at = m.announcedTemplates.get(template);
            return at != null && System.currentTimeMillis() - at < ANNOUNCED_WINS_MS;
        }).orElse(false);
    }

    // ------------------------------------------------------------------ packets

    @Override
    public boolean onPacket(String from, Channel channel, CloudPacket packet) {
        switch (packet) {
            case NodeDataWritePacket write -> {
                if (!cluster.isHead()) {
                    cluster.respond(channel, write, new HeadDataWriteResultPacket(false, "This node is not the head (anymore)."));
                    return true;
                }
                execute(() -> handleWrite(from, channel, write));
                return true;
            }
            case HeadDataChangedPacket changed -> {
                if (!from.equals(cluster.election().head())) {
                    CloudLogger.get().debug("Ignoring a data change from {}, which is not head", from);
                    return true;
                }
                execute(() -> applyChange(from, changed.getKind(), changed.getKey(), changed.getDocument()));
                return true;
            }
            case NodeDataSnapshotRequestPacket request -> {
                execute(() -> cluster.respond(channel, request, new HeadDataSnapshotPacket(buildSnapshot())));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void handleWrite(String from, Channel channel, NodeDataWritePacket write) {
        String op = write.getOperation();
        String target = write.getTarget();
        CloudLogger.get().info("Applying §b{} §8(§b{}§8) §rrequested on §b{}§r.", op, target, from);
        try {
            if (op.startsWith("+") || op.startsWith("-")) {
                boolean present = op.startsWith("+");
                CloudProvider provider = CloudProvider.current();
                Promise<Void> result = switch (op.substring(1)) {
                    case KIND_WHITELIST -> present ? provider.addToWhitelist(target) : provider.removeFromWhitelist(target);
                    case KIND_NOTIFICATIONS -> present ? provider.enablePlayerNotifications(target) : provider.disablePlayerNotifications(target);
                    default -> throw new IllegalArgumentException("unknown operation " + op);
                };
                result.thenSuccess(_ -> cluster.respond(channel, write, new HeadDataWriteResultPacket(true, "")))
                        .failure(ex -> cluster.respond(channel, write, new HeadDataWriteResultPacket(false, String.valueOf(ex.getMessage()))));
                return;
            }

            TemplateManager templates = PocketCloud.instance().templates();
            ServerGroupManager groups = PocketCloud.instance().serverGroups();
            switch (op) {
                case OP_TEMPLATE_CREATE -> {
                    if (templates.check(target)) throw new IllegalArgumentException("The template " + target + " already exists.");
                    templates.createTemplate(Template.read(document(write.getPayload())));
                }
                case OP_TEMPLATE_EDIT -> templates.edit(template(target), TemplateEditData.read(document(write.getPayload())));
                case OP_TEMPLATE_DELETE -> templates.delete(template(target));
                case OP_GROUP_CREATE -> {
                    if (groups.check(target)) throw new IllegalArgumentException("The group " + target + " already exists.");
                    groups.createGroup(ServerGroup.read(document(write.getPayload())));
                }
                case OP_GROUP_ADD_TEMPLATE -> groups.addTemplate(group(target), template(write.getPayload()));
                case OP_GROUP_REMOVE_TEMPLATE -> groups.removeTemplate(group(target), template(write.getPayload()));
                case OP_GROUP_DELETE -> groups.delete(group(target));
                default -> throw new IllegalArgumentException("unknown operation " + op);
            }
            cluster.respond(channel, write, new HeadDataWriteResultPacket(true, ""));
        } catch (RuntimeException e) {
            CloudLogger.get().warn("Could not apply §b{} §8(§b{}§8) §rfrom §b{}§r: {}", op, target, from, e.getMessage());
            cluster.respond(channel, write, new HeadDataWriteResultPacket(false, String.valueOf(e.getMessage())));
        }
    }

    private static ITemplate template(String name) {
        return PocketCloud.instance().templates().get(name)
                .orElseThrow(() -> new IllegalArgumentException("The template " + name + " does not exist on the head."));
    }

    private static IServerGroup group(String name) {
        return PocketCloud.instance().serverGroups().get(name)
                .orElseThrow(() -> new IllegalArgumentException("The group " + name + " does not exist on the head."));
    }

    private static Map<String, Object> document(String json) {
        Map<String, Object> document = json == null || json.isEmpty() ? null : FileUtils.GSON.fromJson(json, DOCUMENT);
        if (document == null) throw new IllegalArgumentException("missing data");
        return document;
    }

    // ------------------------------------------------------------------ applying (all nodes but the head)

    private void applyChange(String head, String kind, String key, String document) {
        switch (kind) {
            case KIND_WHITELIST, KIND_NOTIFICATIONS -> {
                boolean present = Boolean.parseBoolean(document);
                if (applyPlayerList(kind, key, present)) {
                    CloudLogger.get().info("{} §b{} §r{} §8(§rchanged on head §b{}§8)§r.", kind.equals(KIND_WHITELIST) ? "Whitelist§8:" : "Notifications§8:",
                            key, present ? "§aadded" : "§cremoved", head);
                }
            }
            case KIND_TEMPLATE -> applyTemplate(key, document.isEmpty() ? null : document(document), true);
            case KIND_GROUP -> applyGroup(key, document.isEmpty() ? null : document(document), true);
            default -> CloudLogger.get().debug("Unknown data change {}", kind);
        }
    }

    /** @return true if the local state changed */
    private boolean applyPlayerList(String kind, String player, boolean present) {
        boolean whitelist = kind.equals(KIND_WHITELIST);
        LocalCache<String, Boolean> cache = whitelist ? LocalCache.get(WhitelistCache.class) : LocalCache.get(NotificationListCache.class);
        if (cache.contains(player) == present) return false;

        // The JSON files of the whitelist and the notification list are per node: keep them in line with the head,
        // so a later head has the same data. The JSON provider updates the cache (and the bridges) itself.
        if (CloudProvider.persistent() instanceof CloudJsonProvider json) {
            if (whitelist) {
                if (present) json.addToWhitelist(player);
                else json.removeFromWhitelist(player);
            } else {
                if (present) json.enablePlayerNotifications(player);
                else json.disablePlayerNotifications(player);
            }
        } else {
            // MySQL: the head already wrote the shared database.
            if (present) cache.add(player, true);
            else cache.remove(player);
        }
        return true;
    }

    /**
     * Brings the in-memory template up to date. Not written to the provider: templates.json is replicated by the
     * template replication (JSON), MySQL is shared, Redis was written by the head.
     */
    private void applyTemplate(String name, Map<String, Object> document, boolean announced) {
        TemplateManager templates = PocketCloud.instance().templates();
        ITemplate existing = templates.get(name).orElse(null);
        if (announced) announcedTemplates.put(name, System.currentTimeMillis());
        if (document == null) {
            if (existing == null) return;
            templates.remove(existing);
            if (existing instanceof Template t) t.markForRemoval().syncOut();
            CloudLogger.get().info("Template §b{} §rwas §cremoved §ron the head.", name);
            CloudAPI.instance().events().call(new TemplateDeletedEvent(existing));
            return;
        }

        Template fresh = Template.read(document);
        if (existing == null) {
            templates.add(fresh);
            fresh.syncOut();
            CloudLogger.get().info("Template §b{} §rwas §acreated §ron the head and is now available here.", name);
            CloudAPI.instance().events().call(new TemplateCreatedEvent(fresh));
        } else if (!Objects.equals(existing.write(), fresh.write())) {
            var before = Template.read(existing.write()).settings();
            existing.settings().applyFrom(fresh.settings());
            if (existing instanceof Template t) t.syncOut();
            CloudLogger.get().info("Settings of template §b{} §rwere §eupdated §ron the head.", name);
            CloudAPI.instance().events().call(new TemplateEditedEvent(existing, TemplateEditData.between(before, existing.settings())));
        }
    }

    private void applyGroup(String name, Map<String, Object> document, boolean log) {
        ServerGroupManager groups = PocketCloud.instance().serverGroups();
        IServerGroup existing = groups.get(name).orElse(null);
        if (document == null) {
            if (existing == null) return;
            groups.remove(existing);
            if (existing instanceof ServerGroup g) g.markForRemoval().syncOut();
            if (log) CloudLogger.get().info("Server group §b{} §rwas §cremoved §ron the head.", name);
            CloudAPI.instance().events().call(new ServerGroupDeletedEvent(existing));
            return;
        }

        ServerGroup fresh = ServerGroup.read(document);
        if (existing != null && Objects.equals(existing.write(), fresh.write())) return;
        groups.add(fresh);
        fresh.syncOut();
        if (existing == null) {
            if (log) CloudLogger.get().info("Server group §b{} §rwas §acreated §ron the head.", name);
            CloudAPI.instance().events().call(new ServerGroupCreatedEvent(fresh));
        } else {
            if (log) CloudLogger.get().info("Server group §b{} §rwas §eupdated §ron the head §8(§rtemplates§8: §b{}§8)§r.", name, String.join(", ", fresh.templates()));
            CloudAPI.instance().events().call(new ServerGroupUpdatedEvent(fresh, existing.templates(), fresh.templates()));
        }
    }

    // ------------------------------------------------------------------ snapshot

    private boolean replicatedMeta() {
        return CloudProvider.persistent() instanceof CloudJsonProvider;
    }

    private String buildSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put(KIND_WHITELIST, present(LocalCache.get(WhitelistCache.class).getAll()));
        snapshot.put(KIND_NOTIFICATIONS, present(LocalCache.get(NotificationListCache.class).getAll()));
        if (!replicatedMeta()) {
            Map<String, Object> templates = new HashMap<>();
            for (ITemplate t : PocketCloud.instance().templates().getAll()) templates.put(t.name(), t.write());
            Map<String, Object> groups = new HashMap<>();
            for (IServerGroup g : PocketCloud.instance().serverGroups().getAll()) groups.put(g.name(), g.write());
            snapshot.put("templates", templates);
            snapshot.put("groups", groups);
        }
        return FileUtils.GSON.toJson(snapshot);
    }

    private static List<String> present(Map<String, Boolean> map) {
        return map.entrySet().stream().filter(e -> Boolean.TRUE.equals(e.getValue())).map(Map.Entry::getKey).sorted().toList();
    }

    private void requestSnapshot(String head) {
        NodeDataSnapshotRequestPacket request = new NodeDataSnapshotRequestPacket();
        request.then(response -> {
            if (response instanceof HeadDataSnapshotPacket snapshot) {
                execute(() -> {
                    if (!head.equals(cluster.election().head())) return;
                    applySnapshot(head, FileUtils.GSON.fromJson(snapshot.getSnapshot(), SNAPSHOT));
                    snapshotFrom = head;
                });
            }
        }).failure((_, ex, reason) -> CloudLogger.get().debug("Data snapshot from {} failed: {}", head, ex != null ? ex.getMessage() : reason));
        if (!cluster.send(head, request)) CloudLogger.get().debug("Head {} not connected for the data snapshot", head);
    }

    @SuppressWarnings("unchecked")
    private void applySnapshot(String head, Map<String, Object> snapshot) {
        if (snapshot == null) return;
        int changes = 0;
        for (String kind : List.of(KIND_WHITELIST, KIND_NOTIFICATIONS)) {
            if (!(snapshot.get(kind) instanceof List<?> list)) continue;
            Set<String> wanted = new HashSet<>();
            list.forEach(o -> wanted.add(String.valueOf(o)));
            LocalCache<String, Boolean> cache = kind.equals(KIND_WHITELIST) ? LocalCache.get(WhitelistCache.class) : LocalCache.get(NotificationListCache.class);
            Set<String> local = new HashSet<>(present(cache.getAll()));
            for (String player : wanted) if (!local.contains(player) && applyPlayerList(kind, player, true)) changes++;
            for (String player : local) if (!wanted.contains(player) && applyPlayerList(kind, player, false)) changes++;
        }

        if (snapshot.get("templates") instanceof Map<?, ?> templates) {
            for (Map.Entry<?, ?> e : templates.entrySet()) {
                ITemplate before = PocketCloud.instance().templates().get(String.valueOf(e.getKey())).orElse(null);
                Map<String, Object> doc = (Map<String, Object>) e.getValue();
                if (before == null || !Objects.equals(before.write(), Template.read(doc).write())) {
                    applyTemplate(String.valueOf(e.getKey()), doc, false);
                    changes++;
                }
            }
            for (ITemplate t : PocketCloud.instance().templates().getAll()) {
                if (!templates.containsKey(t.name())) {
                    applyTemplate(t.name(), null, false);
                    changes++;
                }
            }
        }
        if (snapshot.get("groups") instanceof Map<?, ?> groups) {
            for (Map.Entry<?, ?> e : groups.entrySet()) applyGroup(String.valueOf(e.getKey()), (Map<String, Object>) e.getValue(), true);
            for (IServerGroup g : PocketCloud.instance().serverGroups().getAll()) {
                if (!groups.containsKey(g.name())) applyGroup(g.name(), null, true);
            }
        }

        if (changes > 0) CloudLogger.get().info("Caught up with head §b{}§r: §b{} §rchange(s) to the whitelist, notifications or templates.", head, changes);
        else CloudLogger.get().debug("Data snapshot from head {}: up to date", head);
    }

    // ------------------------------------------------------------------ manager helpers

    public static boolean forwardTemplateCreate(Template template) {
        return forwardToHead(OP_TEMPLATE_CREATE, template.name(), template.write(), "the creation of template §b" + template.name() + "§r");
    }

    public static boolean forwardTemplateEdit(ITemplate template, TemplateEditData editData) {
        return forwardToHead(OP_TEMPLATE_EDIT, template.name(), editData.write(), "the edit of template §b" + template.name() + "§r");
    }

    public static boolean forwardTemplateDelete(ITemplate template) {
        return forwardToHead(OP_TEMPLATE_DELETE, template.name(), null, "the removal of template §b" + template.name() + "§r");
    }

    public static boolean forwardGroupCreate(ServerGroup group) {
        return forwardToHead(OP_GROUP_CREATE, group.name(), group.write(), "the creation of group §b" + group.name() + "§r");
    }

    public static boolean forwardGroupTemplate(IServerGroup group, ITemplate template, boolean add) {
        return forwardToHead(add ? OP_GROUP_ADD_TEMPLATE : OP_GROUP_REMOVE_TEMPLATE, group.name(), template.name(),
                (add ? "adding §b" : "removing §b") + template.name() + (add ? " §rto" : " §rfrom") + " group §b" + group.name() + "§r");
    }

    public static boolean forwardGroupDelete(IServerGroup group) {
        return forwardToHead(OP_GROUP_DELETE, group.name(), null, "the removal of group §b" + group.name() + "§r");
    }
}
