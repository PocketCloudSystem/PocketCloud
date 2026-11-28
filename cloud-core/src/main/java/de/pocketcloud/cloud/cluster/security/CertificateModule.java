package de.pocketcloud.cloud.cluster.security;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.ClusterModule;
import de.pocketcloud.cloud.cluster.Peer;
import de.pocketcloud.cloud.cluster.security.packet.NodeCertInstallPacket;
import de.pocketcloud.cloud.cluster.security.packet.NodeCertInstallResultPacket;
import de.pocketcloud.cloud.cluster.security.packet.NodeCertKeyRequestPacket;
import de.pocketcloud.cloud.cluster.security.packet.NodeCertKeyResultPacket;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.network.packet.CloudPacket;
import io.netty.channel.Channel;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import de.pocketcloud.cloud.cluster.NodeAlertModule;
import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Renewal of node certificates (cluster spec, phase 7).
 * <ul>
 *     <li>Every node reports serial, validity and whether it holds {@code ca.key} in its heartbeat.</li>
 *     <li>A node warns on the console when its own certificate expires within {@value #WARN_DAYS} days
 *     (and when the cluster CA expires within {@value #CA_WARN_DAYS} days: CA rotation is not supported yet).</li>
 *     <li>{@code node cert renew [node]} on a node holding {@code ca.key}: the target generates a new key pair and
 *     sends the public key ({@link NodeCertKeyRequestPacket}), the issuer signs a certificate and sends it back
 *     ({@link NodeCertInstallPacket}). The private key never leaves the target. The target checks the certificate,
 *     keeps the old one as {@code node.crt.bak}, and uses the new one for new connections; open connections keep running.</li>
 *     <li>The head renews certificates automatically once they are within the renewal window
 *     ({@value #WARN_DAYS} days, or a third of the validity for short-lived certificates), if it holds {@code ca.key}.
 *     Otherwise it warns and names the nodes that can renew.</li>
 * </ul>
 * An expired certificate cannot be renewed this way (the node cannot connect anymore): issue a new join token with
 * {@code node add <name> <address> <port>} on a node holding {@code ca.key}.
 */
public final class CertificateModule implements ClusterModule {

    public static final String STAT_CERT = "cert";
    static final long WARN_DAYS = 30;
    static final long CA_WARN_DAYS = 90;
    private static final long CHECK_INTERVAL_MS = 15_000;
    private static final long PENDING_KEY_MS = 120_000;
    private static final long WARN_REPEAT_MS = 12 * 3_600_000L;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private ClusterManager cluster;
    private ScheduledFuture<?> task;
    private final Map<String, Long> inflight = new ConcurrentHashMap<>();
    private final Map<String, Long> warned = new ConcurrentHashMap<>();
    private volatile PendingKey pendingKey;

    private record PendingKey(KeyPair pair, String requestedBy, long createdAt) {}

    public static Optional<CertificateModule> get() {
        ClusterManager cluster = PocketCloud.instance().cluster();
        if (cluster == null || !cluster.clustered()) return Optional.empty();
        for (ClusterModule module : cluster.modules()) {
            if (module instanceof CertificateModule m && m.cluster != null) return Optional.of(m);
        }
        return Optional.empty();
    }

    @Override
    public Collection<Class<? extends Packet>> packets() {
        return List.of(NodeCertKeyRequestPacket.class, NodeCertKeyResultPacket.class, NodeCertInstallPacket.class, NodeCertInstallResultPacket.class);
    }

    @Override
    public void start(ClusterManager cluster) {
        this.cluster = cluster;
        task = cluster.scheduler().scheduleWithFixedDelay(this::checkSafe, 5_000, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (task != null) task.cancel(false);
    }

    // ------------------------------------------------------------------ heartbeat

    @Override
    public void heartbeatStats(Map<String, Object> stats) {
        if (cluster == null) return;
        X509Certificate own = certificates().nodeCertificate();
        if (own == null) return;
        Map<String, Object> cert = new LinkedHashMap<>();
        cert.put("serial", ClusterCertificates.serialOf(own));
        cert.put("notBefore", own.getNotBefore().getTime());
        cert.put("notAfter", own.getNotAfter().getTime());
        cert.put("caKey", certificates().hasCaKey());
        stats.put(STAT_CERT, cert);
    }

    /** Certificate data a node reported in its heartbeat (serial, notBefore, notAfter, caKey), or null. */
    public CertStatus statusOf(String node) {
        if (cluster == null) return null;
        if (node.equals(cluster.nodeName())) {
            X509Certificate own = certificates().nodeCertificate();
            return new CertStatus(ClusterCertificates.serialOf(own), own.getNotBefore().getTime(), own.getNotAfter().getTime(), certificates().hasCaKey());
        }
        Peer peer = cluster.peer(node).orElse(null);
        if (peer == null || !(peer.stats().get(STAT_CERT) instanceof Map<?, ?> map)) return null;
        if (!(map.get("notAfter") instanceof Number notAfter) || !(map.get("notBefore") instanceof Number notBefore)) return null;
        return new CertStatus(String.valueOf(map.get("serial")), notBefore.longValue(), notAfter.longValue(), Boolean.TRUE.equals(map.get("caKey")));
    }

    public record CertStatus(String serial, long notBefore, long notAfter, boolean caKey) {

        /** Renewal window: 30 days before expiry, or the last third of the validity for short-lived certificates. */
        public boolean due(long now) {
            long window = Math.min(Duration.ofDays(WARN_DAYS).toMillis(), (notAfter - notBefore) / 3);
            return notAfter - now <= window;
        }

        public boolean expiresSoon(long now) {
            return notAfter - now <= Duration.ofDays(WARN_DAYS).toMillis();
        }
    }

    /** Connected nodes that reported holding {@code ca.key} (including this one). */
    public List<String> caKeyHolders() {
        List<String> holders = new ArrayList<>();
        if (cluster == null) return holders;
        if (certificates().hasCaKey()) holders.add(cluster.nodeName());
        for (Peer peer : cluster.peers()) {
            if (!peer.connected()) continue;
            CertStatus status = statusOf(peer.name());
            if (status != null && status.caKey()) holders.add(peer.name());
        }
        return holders;
    }

    // ------------------------------------------------------------------ periodic check

    private void checkSafe() {
        try {
            check();
        } catch (Throwable t) {
            CloudLogger.get().exception("Certificate check failed", t);
        }
    }

    private void check() {
        if (cluster == null) return;
        long now = System.currentTimeMillis();
        warned.values().removeIf(time -> now - time > WARN_REPEAT_MS);

        CertStatus own = statusOf(cluster.nodeName());
        boolean autoRenewOwn = cluster.isHead() && certificates().hasCaKey() && own.due(now);
        if (own.notAfter() <= now) {
            if (warnOnce("expired:" + own.serial())) {
                NodeAlertModule.alert("Certificate expired", "The certificate of `" + cluster.nodeName() + "` has expired; other nodes reject it.", Color.RED);
                CloudLogger.get().error("The certificate of this node §chas expired§r, other nodes reject it. Run §bnode add {} <address> <port> §ron a node with ca.key "
                        + "and start this node once with §b--join <token>§r.", cluster.nodeName());
                CloudLogger.get().debug("Own certificate (serial {}) expired on {}.", own.serial(), DATE.format(Instant.ofEpochMilli(own.notAfter())));
            }
        } else if (own.expiresSoon(now) && !autoRenewOwn && warnOnce("own:" + own.serial())) {
            NodeAlertModule.alert("Certificate expires soon", "The certificate of `" + cluster.nodeName() + "` expires in " + remaining(own.notAfter(), now) + ".", Color.ORANGE);
            CloudLogger.get().warn("The certificate of this node §cexpires in {}§r. {}", remaining(own.notAfter(), now),
                    certificates().hasCaKey() ? "Renew it with §bnode cert renew§r." : "Run §bnode cert renew " + cluster.nodeName() + " §ron a node with ca.key" + holdersHint() + ".");
        }

        X509Certificate ca = certificates().caCertificate();
        if (ca.getNotAfter().getTime() - now <= Duration.ofDays(CA_WARN_DAYS).toMillis() && warnOnce("ca:" + ClusterCertificates.serialOf(ca))) {
            NodeAlertModule.alert("Cluster CA expires soon", "The cluster CA expires in " + remaining(ca.getNotAfter().getTime(), now) + ".", Color.ORANGE);
            CloudLogger.get().warn("The cluster CA §cexpires in {}§r. It cannot be renewed yet, and all node certificates stop working with it.",
                    remaining(ca.getNotAfter().getTime(), now));
            CloudLogger.get().debug("Cluster CA valid until {}.", DATE.format(ca.getNotAfter().toInstant()));
        }

        if (!cluster.isHead()) return;
        inflight.values().removeIf(time -> now - time > 60_000);
        List<String> nodes = new ArrayList<>();
        nodes.add(cluster.nodeName());
        for (Peer peer : cluster.peers()) if (peer.connected() && peer.alive(now)) nodes.add(peer.name());

        for (String node : nodes) {
            CertStatus status = statusOf(node);
            if (status == null || !status.due(now) || inflight.containsKey(node)) continue;
            if (!certificates().hasCaKey()) {
                if (warnOnce("noca:" + node + ":" + status.serial())) {
                    CloudLogger.get().warn("The certificate of node §b{} §cexpires in {}§r and this node cannot renew it. Run §bnode cert renew {} §ron a node with ca.key{}.",
                            node, remaining(status.notAfter(), now), node, holdersHint());
                }
                continue;
            }
            CloudLogger.get().info("Renewing the certificate of node §b{} §8(§rexpires in §e{}§8)§r...", node, remaining(status.notAfter(), now));
            renew(node, "automatic renewal by head " + cluster.nodeName()).whenComplete((message, error) -> {
                if (error != null) CloudLogger.get().warn("Could not renew the certificate of node §b{}§8: §c{}", node, rootMessage(error));
                else {
                    CloudLogger.get().success("Renewed the certificate of node §b{}§r.", node);
                    CloudLogger.get().debug(message);
                }
            });
        }
    }

    private String holdersHint() {
        List<String> holders = caKeyHolders();
        return holders.isEmpty() ? " §8(§rnone is connected§8)§r" : " §8(§b" + String.join("§8, §b", holders) + "§8)§r";
    }

    private boolean warnOnce(String key) {
        return warned.putIfAbsent(key, System.currentTimeMillis()) == null;
    }

    // ------------------------------------------------------------------ renewal (issuer side)

    /**
     * Issues a new certificate for {@code node} (this node or a connected one). Requires {@code ca.key}.
     *
     * @return a message for the user
     */
    public CompletableFuture<String> renew(String node, String reason) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (!certificates().hasCaKey()) {
            List<String> holders = caKeyHolders();
            future.completeExceptionally(new IllegalStateException("This node does not hold the cluster CA key (ca.key)."
                    + (holders.isEmpty() ? "" : " Run it on: " + String.join(", ", holders))));
            return future;
        }
        if (!cluster.members().contains(node)) {
            future.completeExceptionally(new IllegalArgumentException("The node " + node + " is not a member of this cluster."));
            return future;
        }
        if (inflight.putIfAbsent(node, System.currentTimeMillis()) != null) {
            future.completeExceptionally(new IllegalStateException("A renewal for " + node + " is already running."));
            return future;
        }
        future.whenComplete((_, _) -> inflight.remove(node));

        if (node.equals(cluster.nodeName())) {
            try {
                KeyPair pair = ClusterCertificates.newKeyPair();
                X509Certificate certificate = certificates().issueCertificate(node, pair.getPublic());
                install(certificate, pair, "this node");
                future.complete(describe("Renewed the certificate of this node §b" + node + "§r", certificate));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
            return future;
        }

        NodeCertKeyRequestPacket keyRequest = new NodeCertKeyRequestPacket(reason);
        keyRequest.then(response -> {
            if (!(response instanceof NodeCertKeyResultPacket result) || !result.isSuccess()) {
                future.completeExceptionally(new IllegalStateException(response instanceof NodeCertKeyResultPacket r ? r.getError() : "unexpected response"));
                return;
            }
            try {
                PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(result.getPublicKey())));
                X509Certificate certificate = certificates().issueCertificate(node, publicKey);
                NodeCertInstallPacket install = new NodeCertInstallPacket(ClusterCertificates.toPem(certificate));
                install.then(installResponse -> {
                    if (installResponse instanceof NodeCertInstallResultPacket r && r.isSuccess()) {
                        future.complete(describe("Renewed the certificate of node §b" + node + "§r", certificate) + " §8(§rdelivered over the cluster connection§8)");
                    } else {
                        future.completeExceptionally(new IllegalStateException(installResponse instanceof NodeCertInstallResultPacket r ? r.getError() : "unexpected response"));
                    }
                }).failure((_, ex, why) -> future.completeExceptionally(ex != null ? ex : new IllegalStateException("no answer from " + node + " (" + why + ")")));
                if (!cluster.send(node, install)) future.completeExceptionally(new IllegalStateException("node " + node + " is not connected"));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }).failure((_, ex, why) -> future.completeExceptionally(ex != null ? ex : new IllegalStateException("no answer from " + node + " (" + why + ")")));
        if (!cluster.send(node, keyRequest)) future.completeExceptionally(new IllegalStateException("node " + node + " is not connected"));
        return future;
    }

    // ------------------------------------------------------------------ renewal (target side)

    @Override
    public boolean onPacket(String from, Channel channel, CloudPacket packet) {
        switch (packet) {
            case NodeCertKeyRequestPacket request -> {
                try {
                    KeyPair pair = ClusterCertificates.newKeyPair();
                    pendingKey = new PendingKey(pair, from, System.currentTimeMillis());
                    CloudLogger.get().debug("Node {} renews the certificate of this node ({}): generated a new key.", from, request.getReason());
                    cluster.respond(channel, request, new NodeCertKeyResultPacket(true, null, Base64.getEncoder().encodeToString(pair.getPublic().getEncoded())));
                } catch (Exception e) {
                    cluster.respond(channel, request, new NodeCertKeyResultPacket(false, e.getMessage(), null));
                }
            }
            case NodeCertInstallPacket request -> {
                PendingKey pending = pendingKey;
                try {
                    if (pending == null || System.currentTimeMillis() - pending.createdAt() > PENDING_KEY_MS || !pending.requestedBy().equals(from)) {
                        throw new IllegalStateException("no matching key request from " + from);
                    }
                    X509Certificate certificate = ClusterCertificates.parseCertificate(request.getCertificatePem());
                    install(certificate, pending.pair(), from);
                    pendingKey = null;
                    cluster.respond(channel, request, new NodeCertInstallResultPacket(true, null));
                } catch (Exception e) {
                    CloudLogger.get().warn("§cRejected §ra new certificate from §b{}§8: §c{}", from, e.getMessage());
                    cluster.respond(channel, request, new NodeCertInstallResultPacket(false, e.getMessage()));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void install(X509Certificate certificate, KeyPair pair, String issuedBy) throws Exception {
        X509Certificate old = certificates().nodeCertificate();
        certificates().installRenewed(certificate, pair.getPrivate(), pair.getPublic(), cluster.nodeName());
        cluster.reloadTls();
        // renewed on this node: the command or the automatic renewal reports it
        if (!issuedBy.equals("this node")) {
            CloudLogger.get().success("The certificate of this node was §arenewed §8(§rvalid until §b{}§8)§r.", DATE.format(certificate.getNotAfter().toInstant()));
        }
        CloudLogger.get().debug("Installed a renewed certificate (issued by {}): serial {}, valid until {}. Old certificate (serial {}) kept as node.crt.bak. "
                        + "New connections use the new certificate, open ones keep running.",
                issuedBy, ClusterCertificates.serialOf(certificate), DATE.format(certificate.getNotAfter().toInstant()), ClusterCertificates.serialOf(old));
    }

    // ------------------------------------------------------------------ helpers

    private ClusterCertificates certificates() {
        return cluster.certificates();
    }

    private static String describe(String prefix, X509Certificate certificate) {
        return prefix + ": serial §b" + ClusterCertificates.serialOf(certificate) + "§r, valid until §b" + DATE.format(certificate.getNotAfter().toInstant()) + "§r";
    }

    public static String formatDate(long epochMillis) {
        return DATE.format(Instant.ofEpochMilli(epochMillis));
    }

    public static String remaining(long notAfter, long now) {
        long ms = notAfter - now;
        if (ms <= 0) return "§cexpired";
        Duration d = Duration.ofMillis(ms);
        if (d.toDays() >= 1) return d.toDays() + " day(s)";
        if (d.toHours() >= 1) return d.toHours() + " h " + d.toMinutesPart() + " min";
        return d.toMinutes() + " min " + d.toSecondsPart() + " s";
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error instanceof java.util.concurrent.CompletionException && error.getCause() != null ? error.getCause() : error;
        return String.valueOf(cause.getMessage());
    }
}
