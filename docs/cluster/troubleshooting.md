# Troubleshooting

Log messages are quoted without color codes. Start with `node list` on two different nodes: they should show the
same members, the same head and the same term, and every node `ALIVE`.

## No head / no majority

```text
Only 1/3 nodes reachable, no majority. This node is isolated.
Lost the majority (1/2), stepping down as head.
Cannot start Lobby: there is no head right now.
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
No node can start a server of Lobby right now: node-1: memory budget 256 MB < 512 MB; node-2: draining; node-3: not in the template's nodes
```

Every member is listed with the reason it was skipped (logged at most once a minute per template):

| Reason | Fix |
|--------|-----|
| `not connected`, `SUSPECT`, `DEAD` | The node is not reachable; see above. |
| `state JOINING` / `state ISOLATED` | The node is starting or has no majority. |
| `draining` | `node undrain <node>`. |
| `not in the template's nodes` | `template nodes <template> <node,...>` or `all`. |
| `static servers live on <node>` | The template has static servers; only its `staticNode` may run them. |
| `software <name> missing` | The node has not downloaded that software (or it is disabled). Check the software folder on that node; `node info <node>` shows what it reports. |
| `memory budget X MB < Y MB` | Raise `maxMemoryMb` in that node's `node.yml` (0 = unlimited), lower the template's `maxMemory`, or stop servers. |
| `CPU 91 % >= 85 %` | The node is busy; it gets servers again when the load drops. |
| `failed to start it just now` | Only during a retry after a failed start. |

Also check these warnings, which disable a template completely in a cluster:

```text
The proxy template Proxy has no nodes set and is disabled in the cluster. Use template nodes Proxy <node,...> to choose the nodes for its proxies.
The static template SkyBlock has no staticNode set and is disabled in the cluster. Use template staticnode SkyBlock <node> to choose the node its static servers live on.
```

## not a member of this cluster

```text
Rejected node connection from 10.0.0.14:51234 (node-4): not a member of this cluster
Node at 10.0.0.11:3657 rejected the connection: not a member of this cluster
```

- The node was removed with `node remove`. Add it again (`node add`) and re-join it, or shut it down.
- The node was added while some members were offline. The member list spreads with the heartbeats; once the
  other members have the new list, the connection is accepted. This fixes itself within seconds after all nodes
  are connected to at least one node that has the newer list.

Other reasons in the same messages: `name does not match certificate` (the name in `node.yml` differs from the
certificate), `same name as this node` (two nodes with the same name), `protocol version X != 1` (different
PocketCloud versions: update all nodes).

## TLS handshake failed

```text
TLS handshake with node connection from 10.0.0.14 failed: ...
```

The peer did not present a certificate of this cluster's CA. Causes:

- The node belongs to another cluster (its `storage/internal/cluster/` was created by a different CA, e.g. after
  the CA node's folder was reset). Join it again with a token from the current CA node.
- The node certificate expired (825 days after `node add`), or the clocks differ by a lot. Fix the clock; renew the
  certificate as described in [Security](security.md#certificate-lifetime).
- Something else (a port scanner, a load balancer health check) connects to the cluster port. Harmless.

## node.yml name does not match the node certificate

```text
node.yml name node-2 does not match the node certificate node-3. Cluster disabled.
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
Cannot become head while Redis is unreachable: ...
Redis already knows a newer head term than 3.
```

With `redis.enabled: true` a node only becomes head after storing its term in Redis, and only if Redis does not
already contain a higher one.

- Unreachable: fix the connection; meanwhile there is no head.
- Newer term: Redis contains the term of an **older** cluster with the same `cloudName` (for example after
  `storage/internal/cluster/` was deleted on all nodes). Delete the keys `pc:<cloudName>:term` and
  `pc:<cloudName>:head` in Redis, or use another `cloudName`.

## Template sync

| Message | Meaning / fix |
|---------|---------------|
| `Cannot start Lobby-4 for head node-1: template version not synced within 7 s` | The node could not fetch the template in time (big change, slow link). The head retries on another node. If it repeats, check `template sync status` on that node. |
| `... the template Lobby has local changes on node-2 that are not replicated yet` | Files were changed on that node and not sent yet (or the sync is paused there). Wait a few seconds or `template sync resume`. |
| `Conflict in Lobby/<file>: changed on X and on Y at the same time. ...` | The same file was changed on two nodes. The later change won; get the other one back with `template history` / `template rollback`. |
| `The local copy of Lobby differs from the cluster version; replacing it with version N.` | A node that never synced this template had a different copy; the cluster version wins. |
| `Template replication only runs while this node is part of a cluster.` | `history`, `rollback`, `sync` need a cluster. |

`template sync status` shows `outdated (head vN)` for templates a follower has not taken over yet.

## Servers

| Message | Meaning / fix |
|---------|---------------|
| `Giving up the start of a server of Lobby: it failed on node-2 and no other node can take it.` | The start failed and no other node fits. The line before (`Node node-2 could not start Lobby-4: ...`) has the reason. |
| `Cannot stop Lobby-3: node node-2 is not connected.` | The owning node is unreachable. Its servers are treated as offline after 10 s anyway. |
| `Node node-3 reports Lobby-3, but that name belongs to a server on node node-1. Ignoring it.` | A duplicate after a failure; the head stops it. Expected after a partition. |
| `No head decided about Lobby-1 in time, stopping it.` | After a restart the node had no head for `bridgeReconnectTimeout` + 30 s, so it stopped its old servers. |
| `The bind address 127.0.0.1 is not reachable from other nodes, servers bind to 0.0.0.0 instead. ...` | See [Setup → Addresses](setup.md#addresses). |

**Players cannot join a server on another node** (the proxy times out): the game server port (UDP, server port
range) is not reachable from the proxy's node, or the server registered with a wrong address. Check
`advertisedAddress` of the server's node, `addresses.public-address` / `server-bind-address` in its
`server_settings.yml`, and the firewall.

**Servers shut down when their node restarts:** the servers run an old bridge (see [Upgrading](upgrading.md#3-update-the-bridge-required)),
or the restart took longer than `bridgeReconnectTimeout`.
