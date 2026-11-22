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
- **Template history is copied lazily.** Followers copy the history entries when they take over a version and keep
  the files of the versions they applied. A version made while a node was offline is missing in that node's history
  until a node that has it pushes or serves it; a rollback to a version whose files no reachable node has fails.
- **Template sync pause is local** to the node where it was run (it survives restarts). While a template is paused
  on a node with an outdated copy, the head does not start servers of it there.
- **A new head can only take over versions from reachable nodes.** It collects the versions of all nodes it can
  reach when it becomes head; a node that was unreachable then and has a newer version sends it when it reconnects
  (it becomes a new version). If no reachable node can serve a template, the head does not serve it until one does.
- **Template and group definitions with MySQL:** the files are not replicated (all nodes use the database), and
  other nodes do not reload changed template settings while running; restart them after changing definitions.
- **Whitelist and notifications** are stored per node by the `json` provider (see below).
- **`server screen` for servers on other nodes** is limited (see [Commands](commands.md#server-in-a-cluster)).
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
