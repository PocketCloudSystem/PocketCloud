# Failures & recovery

Basic rule: **a failure never stops servers on healthy nodes.** What ran on a failed machine is treated as offline
and replaced by auto-scaling on the other nodes.

Timings: heartbeat every 1 s, a node is `SUSPECT` after 10 s and `DEAD` after 20 s without heartbeat (`nodeTimeout` in
`cluster.yml`). A node that
shuts down cleanly (`exit`) tells the others, so it is `DEAD` immediately.

## Overview

| Case | Detected by | Reaction | When it comes back |
|------|-------------|----------|--------------------|
| A node (not the head) fails | no heartbeat for 10 s | Its servers are treated as offline on all nodes, players on its proxies are removed, auto-scaling starts replacements up to `minServerCount`. Static servers stay offline. | It reports its servers; servers that were written off meanwhile are stopped. |
| The head fails | followers see no heartbeat for 10 s | Election, new head after about 10-15 s. It waits 5 s for the state of the nodes, then treats the old head's servers like a node failure. | The old head sees the higher term and becomes follower. |
| Network partition | nodes see each other as `DEAD` | Only the side with a majority has a head. The other side becomes `ISOLATED`: no starts, no auto-scaling, running servers keep running. | Like a node failure: duplicates are stopped. |
| The node process crashes, the machine runs on | like a node failure | like a node failure; game servers keep running without their node | The node finds its servers of the last run and adopts them if the head did not write them off; otherwise it kills them. |
| Redis fails (if enabled) | connection errors | Running servers and players keep running. No node can become head while Redis is unreachable. | - |
| A game server crashes | as before (process gone) | as before; reported to all nodes | - |

## A node fails

On every other node:

```text
Node node-3 has disconnected. Its servers are offline: Lobby-3, Lobby-4
```

The head then starts replacements where `minServerCount` requires them (`Starting Lobby-5 on node-1...`).
Every node remembers the servers it wrote off (for 30 minutes).

When `node-3` comes back (`Node node-3 has connected.`):

- If only the **network** was gone, the node process kept its servers. Its first report contains them; the head
  stops the ones it wrote off:

  ```text
  Stopping Lobby-3 on node node-3: it was replaced while node-3 was gone.
  ```

  and on `node-3`:

  ```text
  Stopping Lobby-3: it was replaced while node-3 was gone.
  ```

  Players still on those servers lose their connection; they were not visible to the rest of the cluster anyway.

- If a server name now belongs to a server on another node, the report is ignored and the server is stopped:
  `Node node-3 reports a server Lobby-3, but that name belongs to a server on node-1. Ignoring it.`

## The head fails

The followers see the head as `DEAD` after 10 s. The oldest reachable node claims the next term and becomes head
with a majority of votes:

```text
node-2 (this node) is now the head.
```

On the other nodes: `node-2 is now the head.` If the old head shut down cleanly, the followers start the
election right away (`Node node-1 has disconnected.`).

The new head waits 5 s before scheduling so it knows which servers run where, then handles the servers of the old
head like a node failure. Template replication continues with the new head. Before it serves templates it asks
all reachable nodes for their versions and takes over newer ones (see
[Templates](templates.md#head-changes)). This is logged only with debug logging:

```text
Template replication: collected the template versions of 3 node(s) (node-2, node-1, node-3).
Template Lobby was at version 2 here, the cluster has version 3 (from node-3): took it over before serving.
Template replication: serving 4 unit(s) as head (0 new version(s), 1 taken over from other nodes).
```

When the old head comes back it sees the higher term and follows
(`node-2 is the head now. This node is no longer the head.` if it still thought it was head).

## Network partition

Example: 3 nodes, `node-3` loses the connection to `node-1` and `node-2`.

- `node-1` and `node-2` are 2 of 3, a majority. They keep (or elect) a head and replace the servers of `node-3`.
- `node-3` sees 1 of 3 and becomes isolated:

  ```text
  Only 1/3 nodes are reachable. No new servers are started until a majority is back.
  ```

  If `node-3` was head: `Only 1/3 nodes are reachable. This node is no longer the head until a majority is back.` (numbers: reachable/members).

  An isolated node does not start or stop servers for the cluster and does not run auto-scaling. Its running
  servers keep running. `server start` there fails with `Cannot start Lobby: the cluster has no head right now.`

There can never be two heads: two majorities always share at least one node, and a node votes only once per term.

When the partition heals, `node-3` follows the head again and its servers that were replaced are stopped as
described above.

## A node restarts while its servers keep running

Servers started with the `screen` or `tmux` start method (and usually also `proc`) survive a crash or restart of
the cloud process. Every node writes the servers it runs to `storage/internal/cluster/local-servers.json`. On the
next start it checks which of them are still alive (with debug logging:
`Found 2 server(s) of the last run that are still running: Lobby-1 (pid 41233, port 40012), ...`).

- **Single node:** they are adopted right away.
- **Cluster:** the node asks the head. The head answers *adopt* if it did not write the server off and the name is
  still the node's own, otherwise *kill*. The head logs only the kills:

  ```text
  Node node-2 still runs Lobby-2 from before its restart. Stopping it: it was replaced at 14:02:10 while node-2 was gone.
  ```

  On the node:

  ```text
  The server Lobby-1 is still running from the last run and was adopted.
  Stopping Lobby-2 from the last run: another server replaced it in the meantime.
  ```

  If no head decides in time (`bridgeReconnectTimeout` + 30 s), the node stops them:
  `Stopping Lobby-1 from the last run: no head confirmed it in time.`

An adopted server is `STARTING` until its bridge reconnects. The bridge retries every 2 s for
`bridgeReconnectTimeout` seconds (default 60) and shuts the server down if the node does not come back in time
(`No keep alive from the cloud, reconnecting...` in the server log). This needs the cluster-capable bridge, see
[Upgrading](upgrading.md).

Servers of the last run that have exited meanwhile are cleaned up (with debug logging:
`Server Lobby-2 of the last run has exited, cleaning up.`).

## What an operator sees and does

| You see | Meaning | What to do |
|---------|---------|------------|
| `node list` shows a node `DEAD` | No heartbeat for 10 s | Check the machine and the cluster port. Nothing else is needed: its servers are replaced. |
| `node list` header shows `head: none` | No majority | Bring enough nodes back. Running servers keep running. |
| `Only X/Y nodes are reachable. ...` | This node does not see a majority | Check the network between this node and the others. |
| `Node X has disconnected. Its servers are offline: ...` | Normal reaction to a failed node | - |
| `Stopping X on node Y: it was replaced while Y was gone.` | Duplicate after a returning node | - (expected) |
| `No node can start a server of X right now: ...` | No node fits the scheduling rules | See [Troubleshooting](troubleshooting.md#no-node-can-start-a-server-of-x-right-now). |

Planned maintenance of a machine: `node drain <node> --stop`, wait until its servers are replaced, stop the node
with `exit`, do the work, start it again, `node undrain <node>`.
