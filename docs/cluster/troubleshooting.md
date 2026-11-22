# Troubleshooting

Log messages are quoted without color codes. Start with `node list` on two different nodes: they should show the
same members, the same head and the same term, and every node `ALIVE`.

## No head / no majority

```text
Only 1/3 nodes are reachable. No new servers are started until a majority is back.
Only 1/3 nodes are reachable. This node is no longer the head until a majority is back.
Cannot start Lobby: the cluster has no head right now.
```

`node list` shows `head: none`; HTTP writes answer `503 There is no head node right now.`

The node sees fewer than `members / 2 + 1` nodes. Running servers keep running, but nothing is started or replaced.

- Check that the other nodes run and that **TCP `clusterPort` (3657) is open in both directions** between all nodes.
- Check that `node list` shows the right addresses. They come from `node add`. To fix one, run
  `node add <name> <correct address> [port]` again on the CA node (this updates the member list on all nodes) and
  set the same `advertisedAddress`/`clusterPort` in that node's `node.yml` (or re-join it with the new token).
- With **2 members** one failed node is enough to lose the majority. Use 3 nodes, or `node remove` a node that is
  gone for good (this lowers the majority).
- Right after the first `node add` this is expected until the new node has joined.

## No node can start a server of X right now

```text
No node can start a server of Lobby right now: node-1: not enough memory (256 MB free, 512 MB needed); node-2: draining; node-3: not in the template's nodes
```

Every member is listed with the reason it was skipped (logged at most once a minute per template):

| Reason | Fix |
|--------|-----|
| `not connected`, `not responding` | The node is not reachable; see above. |
| `not ready (joining)` / `not ready (isolated)` | The node is starting or has no majority. |
| `draining` | `node undrain <node>`. |
| `not in the template's nodes` | `template nodes <template> <node,...>` or `all`. |
| `static servers live on <node>` | The template has static servers; only its `staticNode` may run them. |
| `software <name> not installed` | The node has not downloaded that software (or it is disabled). Check the software folder on that node; `node info <node>` shows what it reports. |
| `not enough memory (X MB free, Y MB needed)` | Raise `maxMemoryMb` in that node's `node.yml` (0 = unlimited), lower the template's `maxMemory`, or stop servers. |
| `CPU busy (91 %)` | The CPU load of the node is over the limit; it gets servers again when the load drops. |
| `failed to start it just now` | Only during a retry after a failed start. |

Also check these warnings, which disable a template completely in a cluster:

```text
Proxies of Proxy cannot start until you choose their nodes with template nodes Proxy <node,...>.
Static servers of SkyBlock cannot start until you choose their node with template staticnode SkyBlock <node>.
```

## not a member of this cluster

```text
Refused the connection of node node-4 (10.0.0.14:51234): not a member of this cluster
The node at 10.0.0.11:3657 refused the connection: not a member of this cluster
```

- The node was removed with `node remove`. Add it again (`node add`) and re-join it, or shut it down.
- The node was added while some members were offline. The member list spreads with the heartbeats; once the
  other members have the new list, the connection is accepted. This fixes itself within seconds after all nodes
  are connected to at least one node that has the newer list.

Other reasons in the same messages: `name does not match certificate` (the name in `node.yml` differs from the
certificate), `same name as this node` (two nodes with the same name), `different cloud version (protocol X, expected 1)`
(update all nodes to the same version).

## Could not connect securely

```text
Could not connect securely with 10.0.0.14. Is its certificate from this cluster and still valid?
```

The TLS handshake failed: the peer did not present a certificate of this cluster's CA (the exact error is logged
with debug logging). Causes:

- The node belongs to another cluster (its `storage/internal/cluster/` was created by a different CA, e.g. after
  the CA node's folder was reset). Join it again with a token from the current CA node.
- The node certificate expired (825 days after `node add`), or the clocks differ by a lot. Fix the clock; renew the
  certificate as described in [Security](security.md#certificate-lifetime).
- Something else (a port scanner, a load balancer health check) connects to the cluster port. Harmless.

## The name in node.yml does not match the certificate

```text
The name node-2 in node.yml does not match the certificate of this node (node-3). Running as a single node.
```

The node runs alone. Set `name` in `node.yml` back to the name in the certificate (the name given to `node add`).
A node cannot be renamed; add it under the new name instead.

## Join problems

| Message | Fix |
|---------|-----|
| `Invalid join token` (with the exception) | The token was cut off or changed. Quote it in the shell (`--join '<token>'`) and copy the whole line after `--join`. |
| `Only the node holding the cluster CA key (ca.key) can add nodes.` | Run `node add` on the node that created the cluster. |
| `Failed to start the cluster port 0.0.0.0:3657` | The port is in use or the bind address is wrong (`clusterBindAddress`, `clusterPort`). |

## Redis

```text
This node cannot become the head while Redis is unreachable: ...
Replaced the head of another cluster with the same cloud name in Redis. If two clusters share this Redis, give each its own cloudName.
```

With debug logging also:

```text
Redis already knows a newer head term than 3
Redis held head term 100 of another cluster (id deadbeef, this cluster 3ef2...); replaced it with term 10
Redis knows head term 500, but no member of this cluster reports it (this node: 11). Claiming term 501 next
```

With `redis.enabled: true` a node only becomes head after storing its term in Redis, and only if Redis does not
already contain a higher term of the same cluster. The term is stored together with the cluster id (serial number
of the cluster CA certificate).

- Unreachable: fix the connection; meanwhile there is no head.
- Term of another cluster (an earlier cluster with the same `cloudName`, e.g. after `storage/internal/cluster/` was
  deleted on all nodes, or a term written by a version without the cluster id): it is replaced at once with a
  warning.
- Newer term of this cluster that no reachable member reports (e.g. the term files were lost): the candidate skips
  it and claims the stored term + 1 about 2 s later.
- `Redis already knows a newer head term than N` (debug) alone means a member really holds that term (a head is active);
  the node follows it.

## Template sync

| Message | Meaning / fix |
|---------|---------------|
| `Could not start Lobby-4: the template could not be synced within 7 s` | The node could not fetch the template in time (big change, slow link). The head retries on another node. If it repeats, check `template sync status` on that node. |
| `... the template Lobby has local changes on node-2 that are not replicated yet` | Files were changed on that node and not sent yet. Wait a few seconds. |
| `... the sync of Lobby is paused on node-2 and it does not have version N` | The sync is paused there and that node's copy is not the current version: the head starts the server elsewhere. Afterwards the head skips that node for the template (`template sync paused there` in `No node can start a server ...`) until `template sync resume`. |
| `This node has a newer copy of template Lobby than the head; keeping it and sending it to the cluster.` | The head could not reach this node when it collected the versions. The node does not let the older version overwrite its copy; it sends it and the head makes it a new version. |
| `The newest version of template Lobby is on no reachable node; using the copy of this node.` | The new head is behind, and the node(s) that have the newer version are not reachable. Check `template history Lobby` afterwards; the newer version is merged when such a node reconnects. |
| `Template Lobby is missing here and no reachable node has it. It is not deleted; a node that has it sends it later.` | The new head has no copy and no reachable node has one. The template is not deleted; a node that has it offers it again on its next full sync. |
| `Denied server handshake request from Lobby-1 (...): it is an old process of that server.` | A process of an older server with the same name (e.g. an orphan that is being stopped) tried to register. It is refused and shuts down; the real server is not affected. |
| `Conflict in Lobby/<file>: changed on X and Y at the same time. The change from X is kept, the other one stays in the history.` | The same file was changed on two nodes. The later change won; get the other one back with `template history` / `template rollback`. |
| `The local copy of template Lobby differs from the cluster and is replaced by the cluster version.` | A node that never synced this template had a different copy; the cluster version wins. |
| `Template replication only runs while this node is part of a cluster.` | `history`, `rollback`, `sync` need a cluster. |

`template sync status` shows `outdated (head vN)` for templates a follower has not taken over yet.

## Servers

| Message | Meaning / fix |
|---------|---------------|
| `Could not start a server of Lobby: it failed on node-2 and no other node can take it.` | The start failed and no other node fits. The line before (`node-2 could not start Lobby-4: ...`) has the reason. |
| `Cannot stop Lobby-3: node node-2 is not connected.` | The owning node is unreachable. Its servers are treated as offline after 10 s anyway. |
| `Node node-3 reports a server Lobby-3, but that name belongs to a server on node-1. Ignoring it.` | A duplicate after a failure; the head stops it. Expected after a partition. |
| `Stopping Lobby-1 from the last run: no head confirmed it in time.` | After a restart the node had no head for `bridgeReconnectTimeout` + 30 s, so it stopped its old servers. |
| `The bind address 127.0.0.1 is not reachable from other nodes, servers bind to 0.0.0.0 instead. ...` | See [Setup → Addresses](setup.md#addresses). |

**Players cannot join a server on another node** (the proxy times out): the game server port (UDP, server port
range) is not reachable from the proxy's node, or the server registered with a wrong address. Check
`advertisedAddress` of the server's node, `addresses.public-address` / `server-bind-address` in its
`server_settings.yml`, and the firewall.

**Servers shut down when their node restarts:** the servers run an old bridge (see [Upgrading](upgrading.md#3-update-the-bridge-required)),
or the restart took longer than `bridgeReconnectTimeout`.

**`Unable to connect to downstream <server>` with `Signaling connection failed: ... could not be reached`** (proxy log,
server on another machine): the proxy reaches the server's node by its public address, but the port is blocked.
`connect timed out` means the packets are dropped (cloud firewall, e.g. Oracle Cloud security list or NSG);
`connection refused` means they arrive but are rejected (local iptables, e.g. Oracle's default `REJECT` rule, or the
port lies outside the opened range). Open the server port range for **TCP and UDP** from the proxy nodes in both
places, and check with `ss -tulpn | grep <port>` that the server listens on `0.0.0.0`.
