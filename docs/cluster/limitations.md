# Known limitations

- **No live migration.** A running server stays on its node until it stops.
- **2 nodes do not give fault tolerance.** The majority of 2 is 2; use 3 or more nodes (see
  [Overview](overview.md#the-head-node-model)).
- **About 20 nodes at most.** Every node connects to every other node.
- **Replacing servers of a failed node is not instant**: the node is declared dead after 10 s, a new head needs
  about 10-15 s plus 5 s before it schedules.
- **A network-only failure produces duplicates for a short time.** Servers on the cut-off node keep running and
  are stopped when the node comes back, after their replacements have started. Players on them are disconnected.
- **Only the node that created the cluster can add nodes** (it holds `ca.key`). If that machine is lost, existing
  nodes keep working, but adding nodes requires a new CA. Back up `ca.key`.
- **No certificate revocation list.** `node remove` revokes by membership only; re-adding a removed name accepts its
  old certificate again (see [Security](security.md#removing-a-node)).
- **Join tokens appear in the console log** of the node that ran `node add`.
- **Template history lives on the head that recorded it** and does not move to a new head.
- **Template sync pause is local** to the node where it was run and is lost on restart; servers starting on that
  node use the unfinished local copy.
- **The new head's template copies are the reference.** After a head change the new head serves the versions it
  has on disk; changes that had not reached it before the old head failed are lost from the cluster version (they
  stay in the old head's history).
- **Template and group definitions with MySQL:** the files are not replicated (all nodes use the database), and
  other nodes do not reload changed template settings while running; restart them after changing definitions.
- **Whitelist and notifications** are stored per node by the `json` provider (see below).
- **`server screen` and `server info` for servers on other nodes** are limited (see below).
- **Redis itself is not highly available** (use Redis Sentinel or a managed Redis if you depend on it).
- **No node events** (`NodeJoinedEvent` etc.) for plugins yet; poll `CloudAPI.nodes()`.
- **Software and bridge jars are not replicated**; every node downloads them itself.
- **`template push`** (manual push of a template to all nodes) does not exist; the automatic sync replaces it.

## Coming soon

> **Work in progress.** The following features are being added and are **not** part of this version yet. This
> section will be replaced by documentation once they are merged.

- **Live logs of remote servers in `server screen`**: streaming the output of a server on another node to the
  console of the node you are on.
- **Certificate renewal commands**: `node cert info|renew` to show the validity of the node certificates and renew
  them without a full re-join.
- **Persisted written-off list**: the servers a node wrote off after a node failure are kept across restarts
  (today: in memory for 30 minutes), so duplicates are also caught after a restart of the head.
- **Cluster-wide whitelist and notification writes**: changes to the whitelist and the notification list made on
  one node are applied on all nodes.
