package de.pocketcloud.cloud.cluster;

import de.pocketcloud.cloud.cluster.net.packet.HeadAnnouncePacket;
import de.pocketcloud.cloud.cluster.net.packet.HeadClaimPacket;
import de.pocketcloud.cloud.cluster.net.packet.HeadVotePacket;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.common.util.FileUtils;
import com.google.gson.reflect.TypeToken;
import io.netty.channel.Channel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Head election without a database.
 * <ul>
 *     <li>A head stays head as long as it sees a majority of the members (sticky).</li>
 *     <li>Without a living head, the oldest living node ({@code startedAt}, then name) claims the next term.</li>
 *     <li>A node votes at most once per term and only for the node it also considers the oldest living one.</li>
 *     <li>A candidate needs votes from a majority (its own counts). Two majorities always overlap, so there
 *     can never be two heads in the same term. Higher terms always win (fencing).</li>
 * </ul>
 * All methods are synchronized; they are called from the cluster scheduler and the Netty threads.
 */
public final class HeadElection {

    private static final long STARTUP_GRACE_MS = 3_000;
    private static final long CLAIM_TIMEOUT_MS = 3_000;

    private final ClusterManager cluster;
    private final Path termFile;
    private final long startedAt = System.currentTimeMillis();

    private NodeState state = NodeState.JOINING;
    private long term;
    private long votedTerm;
    private String head;

    private long claimTerm;
    private long claimDeadline;
    private final Set<String> votes = new HashSet<>();
    private long nextAttempt;
    private long headSince;

    HeadElection(ClusterManager cluster, Path termFile) {
        this.cluster = cluster;
        this.termFile = termFile;
        loadTerm();
    }

    public synchronized NodeState state() {
        return state;
    }

    public synchronized long term() {
        return term;
    }

    public synchronized String head() {
        return head;
    }

    public long startedAt() {
        return startedAt;
    }

    public synchronized boolean isHead() {
        return state == NodeState.HEAD;
    }

    /** Time this node became head (0 in single-node mode). */
    public synchronized long headSince() {
        return headSince;
    }

    /** Called every 250 ms. */
    public synchronized void tick() {
        long now = System.currentTimeMillis();
        String self = cluster.nodeName();
        List<String> alive = cluster.aliveNodes(now);
        int quorum = cluster.quorum();

        if (!cluster.members().contains(self)) {
            // Joined, but the member list of the cluster has not arrived yet.
            state = NodeState.JOINING;
            return;
        }

        if (state == NodeState.HEAD) {
            if (alive.size() < quorum) {
                CloudLogger.get().warn("Only §b{}§8/§b{} §rnodes are reachable. This node is §cno longer the head §runtil a majority is back.", alive.size(), cluster.members().size());
                state = NodeState.ISOLATED;
                head = null;
            }
            return;
        }

        if (state == NodeState.CANDIDATE) {
            if (now < claimDeadline) return;
            CloudLogger.get().debug("Head claim for term {} failed ({} votes)", claimTerm, votes.size());
            state = NodeState.FOLLOWER;
            nextAttempt = now + ThreadLocalRandom.current().nextLong(1_000, 3_000);
        }

        if (head != null && !head.equals(self) && alive.contains(head)) {
            state = NodeState.FOLLOWER;
            return;
        }

        if (now - startedAt < STARTUP_GRACE_MS) {
            state = NodeState.JOINING;
            return;
        }

        if (alive.size() < quorum) {
            if (state != NodeState.ISOLATED) {
                CloudLogger.get().warn("Only §b{}§8/§b{} §rnodes are reachable. No new servers are started until a majority is back.", alive.size(), cluster.members().size());
            }
            state = NodeState.ISOLATED;
            head = null;
            return;
        }

        if (state == NodeState.ISOLATED || state == NodeState.JOINING) state = NodeState.FOLLOWER;

        String candidate = cluster.oldest(alive);
        if (self.equals(candidate) && now >= nextAttempt) startClaim(now, alive, quorum);
    }

    private void startClaim(long now, List<String> alive, int quorum) {
        claimTerm = Math.max(term, votedTerm) + 1;
        votedTerm = claimTerm;
        saveTerm();
        votes.clear();
        votes.add(cluster.nodeName());
        state = NodeState.CANDIDATE;
        claimDeadline = now + CLAIM_TIMEOUT_MS;
        CloudLogger.get().debug("Claiming head for term {}", claimTerm);

        if (votes.size() >= quorum) {
            becomeHead(claimTerm);
            return;
        }

        long thisTerm = claimTerm;
        for (String peer : alive) {
            if (peer.equals(cluster.nodeName())) continue;
            cluster.sendRequest(peer, new HeadClaimPacket(thisTerm, startedAt), response -> {
                if (response instanceof HeadVotePacket vote) onVote(peer, thisTerm, vote);
            });
        }
    }

    private synchronized void onVote(String from, long requestedTerm, HeadVotePacket vote) {
        if (vote.getTerm() > Math.max(term, requestedTerm)) {
            adoptTerm(vote.getTerm(), null);
            return;
        }

        if (state != NodeState.CANDIDATE || requestedTerm != claimTerm || !vote.isGranted()) return;
        votes.add(from);
        if (votes.size() >= cluster.quorum()) becomeHead(claimTerm);
    }

    private void becomeHead(long newTerm) {
        if (!cluster.fenceHead(newTerm)) {
            state = NodeState.FOLLOWER;
            nextAttempt = System.currentTimeMillis() + 2_000;
            return;
        }

        term = newTerm;
        head = cluster.nodeName();
        state = NodeState.HEAD;
        headSince = System.currentTimeMillis();
        saveTerm();
        CloudLogger.get().success("§b{} §8(§rthis node§8) §ris now the §bhead§r.", head);
        CloudLogger.get().debug("Head term: {}", term);
        cluster.broadcast(() -> new HeadAnnouncePacket(newTerm));
        cluster.onHeadChanged(head);
    }

    /** A candidate asks for our vote. */
    public synchronized HeadVotePacket onClaim(String from, HeadClaimPacket claim) {
        long now = System.currentTimeMillis();
        long t = claim.getTerm();
        boolean headGone = head == null || head.equals(from) || (!head.equals(cluster.nodeName()) && !cluster.aliveNodes(now).contains(head));
        if (state == NodeState.HEAD) headGone = false;

        List<String> alive = new ArrayList<>(cluster.aliveNodes(now));
        if (!alive.contains(from)) alive.add(from);
        boolean oldest = from.equals(cluster.oldest(alive));

        boolean grant = t > term && t > votedTerm && headGone && oldest;
        if (grant) {
            votedTerm = t;
            saveTerm();
        }

        CloudLogger.get().debug("Vote for {} in term {}: {} (headGone={}, oldest={})", from, t, grant, headGone, oldest);
        return new HeadVotePacket(Math.max(term, t), grant);
    }

    public synchronized void onAnnounce(String from, long announcedTerm) {
        if (announcedTerm < term) return;
        boolean changed = !from.equals(head);
        adoptTerm(announcedTerm, from);
        if (changed) {
            CloudLogger.get().info("§b{} §ris now the §bhead§r.", from);
            CloudLogger.get().debug("Head term: {}", announcedTerm);
            cluster.onHeadChanged(from);
        }
    }

    /** Term and head as seen by another node (hello, heartbeat). */
    public synchronized void observe(long peerTerm, String peerHead) {
        if (peerHead == null || peerHead.isEmpty()) {
            if (peerTerm > term) adoptTerm(peerTerm, null);
            return;
        }

        // A node becomes head only through its own claim. A peer that still names this node as head (e.g. after
        // this node stepped down for lack of a majority) must not make it head again.
        if (peerHead.equals(cluster.nodeName()) && state != NodeState.HEAD) {
            if (peerTerm > term) adoptTerm(peerTerm, null);
            return;
        }

        if (peerTerm > term || (peerTerm == term && head == null && state != NodeState.CANDIDATE)) {
            boolean changed = !peerHead.equals(head);
            adoptTerm(peerTerm, peerHead);
            if (changed && !peerHead.equals(cluster.nodeName())) {
                CloudLogger.get().info("§b{} §ris now the §bhead§r.", peerHead);
                cluster.onHeadChanged(peerHead);
            }
        }
    }

    private void adoptTerm(long newTerm, String newHead) {
        if (newTerm > term) {
            term = newTerm;
            saveTerm();
        }

        if (state == NodeState.HEAD && newHead != null && !newHead.equals(cluster.nodeName())) {
            CloudLogger.get().warn("§b{} §ris the head now. This node is §cno longer the head§r.", newHead);
        }

        if (newHead != null) {
            head = newHead;
            state = newHead.equals(cluster.nodeName()) ? NodeState.HEAD : NodeState.FOLLOWER;
        } else if (state == NodeState.HEAD || state == NodeState.CANDIDATE) {
            state = NodeState.FOLLOWER;
            head = null;
        }
    }

    /** A stale newer term (stored in Redis, held by no member) is skipped: the next claim uses {@code staleTerm + 1}. */
    synchronized void skipTo(long staleTerm) {
        if (staleTerm <= votedTerm) return;
        votedTerm = staleTerm;
        saveTerm();
    }

    /** Single-node mode: always head. */
    public synchronized void forceSingleHead() {
        state = NodeState.HEAD;
        head = cluster.nodeName();
    }

    public synchronized void peerLeft(String name) {
        if (name.equals(head) && state != NodeState.HEAD) {
            CloudLogger.get().debug("Head {} is shutting down", name);
            head = null;
        }
    }

    private void loadTerm() {
        try {
            if (!Files.isRegularFile(termFile)) return;
            Map<String, Object> data = FileUtils.GSON.fromJson(Files.readString(termFile), new TypeToken<Map<String, Object>>() {}.getType());
            term = ((Number) data.getOrDefault("term", 0L)).longValue();
            votedTerm = ((Number) data.getOrDefault("votedTerm", 0L)).longValue();
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to read the cluster term", e);
        }
    }

    private void saveTerm() {
        try {
            Files.createDirectories(termFile.getParent());
            Files.writeString(termFile, FileUtils.GSON.toJson(Map.of("term", term, "votedTerm", votedTerm)));
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to save the cluster term", e);
        }
    }
}
