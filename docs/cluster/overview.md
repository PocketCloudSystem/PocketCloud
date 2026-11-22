# Cluster overview

## The head-node model

All machines run the same program (`pocketcloud.jar`). Each running instance is a **node** with a unique name
(`node.yml` → `name`). Nodes connect to each other over TLS on the cluster port (default `3657`), every node to
every other node (full mesh), and send a heartbeat every second.

One node is the **head**. Being head is only an additional role: the head runs servers like every other node, but
it also makes the decisions that must be made in one place.

The head is elected without a database:

- If there is no head, the **oldest reachable node** (smallest start time, then name) asks the others to make it
  head. It needs the votes of a **majority** of all members (its own vote counts).
- A head stays head as long as it sees a majority. This avoids the role jumping around.
- Every new head increases the **term** (a counter stored in `storage/internal/cluster/term.json`). Nodes ignore
  start requests carrying an older term, so a former head that comes back after a network problem cannot give
  orders anymore.
- A node that sees less than a majority becomes **ISOLATED**: it does not start or stop servers for the cluster,
  but its running servers keep running.

Majority = `members / 2 + 1` (rounded down): 1 of 1, 2 of 2, 2 of 3, 3 of 4, 3 of 5.

| Members | Majority | Nodes that may fail while there still is a head |
|---------|----------|-------------------------------------------------|
| 1 | 1 | 0 (single node, always head) |
| 2 | 2 | **0** - if one node is down, there is no head |
| 3 | 2 | 1 |
| 4 | 3 | 1 |
| 5 | 3 | 2 |

**Use at least 3 nodes** if the cluster should survive the failure of a machine. With 2 nodes, losing either one
stops all new server starts until it is back (running servers keep running).

Node states, as shown by `node list`: `JOINING` (just started, collecting the view of the others), `FOLLOWER`,
`CANDIDATE` (asking for votes), `HEAD`, `ISOLATED` (no majority). Peers are additionally `ALIVE`, `SUSPECT`
(no heartbeat for 5 s) or `DEAD` (no heartbeat for 10 s).

## Who does what

| Task | Every node | Only the head |
|------|:----------:|:-------------:|
| Prepare, start, stop, kill its own servers | yes | |
| Reserve ports, keep PIDs and logs, detect crashes (local) | yes | |
| Accept bridge and proxy connections of servers on the same machine | yes | |
| Console and HTTP API (actions with cluster effect go to the head or to the owning node) | yes | |
| Keep a full copy of all templates, report local template changes | yes | |
| Decide on which node a server starts | | yes |
| Auto-scaling (`minServerCount`, `startNewServerThreshold`) | | yes |
| Hand out server IDs | | yes |
| Replace servers of failed nodes | | yes |
| Order and distribute template changes, keep the template history | | yes |
| Change drain mode of nodes | | yes |

## What is shared, and how

| Data | How it is shared |
|------|------------------|
| Member list (names, addresses) | `storage/internal/cluster/members.json` on every node. Changed only by `node add` / `node remove`; every change gets a higher version and spreads with the heartbeats. |
| Servers (name, status, port, node, ...) | Each node reports the servers it runs to all other nodes (on every change and every 10 s). Every node keeps the whole cluster in its cache. |
| Players | Each node shares the players on its proxies with the other nodes. |
| Template files (`templates/<name>`, `templates/global/<type>`, `groups/<name>`) | Full copy on every node, kept in sync by the head, see [Templates & replication](templates.md). |
| Template and group settings | With the `json` provider, `templates/templates.json` and `groups/groups.json` are replicated like template files. With `mysql`, all nodes use the same database. |
| Drain mode | Set by the head, sent with every heartbeat, stored in `storage/internal/cluster/drain.json` on every node. |
| Server software and the bridge jar | **Not** replicated. Every node downloads them itself, like a single installation. |
| Redis (optional) | If enabled: head term fencing, shared server/player/ID state and a cache in front of the provider. See [Configuration](configuration.md#configyml). |

## Bridges and proxies

The bridge plugin in every server still connects to `127.0.0.1:3656`, i.e. to the node on the **same machine**.
That node answers it from its cache, which contains the whole cluster. Requests with cluster effect (start a server,
stop a server on another node, transfer a player to a server on another node) are relayed to the head or the
owning node. A head change does not affect the bridges.

Proxies get every server of the cluster registered with its **public address** (the `advertisedAddress` of the
node it runs on, see [Setup](setup.md#addresses)), so a proxy on node A can send players to a lobby on node B.

## What is not in a cluster

- No live migration of running servers between nodes.
- No automatic failover of Redis itself.
- Meant for up to about 20 nodes (the mesh has n² connections).

See [Known limitations](limitations.md) for the full list.
