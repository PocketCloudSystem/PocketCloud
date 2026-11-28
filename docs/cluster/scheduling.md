# Scheduling

Only the **head** decides where a server starts. It hands out the server name and ID; the chosen node reserves the
port, makes sure it has the right template version, prepares and starts the server. Auto-scaling
(`minServerCount`, `startNewServerThreshold`, stop on empty) runs unchanged, but only on the head.

`server start <template> [amount]` (or the HTTP API / a bridge request) on any other node is forwarded to the head:

```text
Asked the head node-1 to start 2x Lobby.
```

On the head:

```text
Starting Lobby-3 on node-2...
```

and on the target node the usual `Starting Lobby-3 [...]...` and `The server Lobby-3 has connected to the cloud.`

A head that has just been elected waits 5 s for the state of the other nodes before it schedules, so servers are
not all started on the head.

Every node runs its own prepare and start queue (at most 2 prepares and 2 starts at a time per node).

## Node selection

For every new server the head checks all members. A node is a candidate only if **all** of these hold (the text in
brackets is the reason shown when a node is skipped):

| Filter | Reason when skipped |
|--------|---------------------|
| Connected and `ALIVE` (heartbeat within 5 s) | `not connected`, `not responding` |
| State `FOLLOWER` or `HEAD` | `not ready (joining)`, `not ready (isolated)`, ... |
| Not the node a start of this template just failed on (retry) | `failed to start it just now` |
| Not in drain mode | `draining` |
| Static templates: the node is the template's `staticNode` | `static servers live on <node>` |
| The template's `nodes` is empty or contains the node | `not in the template's nodes` |
| The node has the template's software (jar present; reported in its heartbeat) | `software <name> not installed` |
| Free memory budget ≥ template `maxMemory` (budget = `maxMemoryMb` of the node minus `maxMemory` of the servers on it; `maxMemoryMb: 0` = unlimited) | `not enough memory (X MB free, Y MB needed)` |
| CPU load (from the heartbeat) below 85 % | `CPU busy (91 %)` |

Before the filters, two template rules apply in a cluster:

- a **proxy template without `nodes`** is disabled,
- a **static template without `staticNode`** is disabled.

If no node fits, the head logs why (at most once a minute per template) and starts nothing:

```text
No node can start a server of Lobby right now: node-1: not enough memory (256 MB free, 512 MB needed); node-2: draining; node-3: not connected
```

The ID is given back and auto-scaling tries again on its next run.

`node info <node>` shows the software a node reports and its CPU against the limit
(`CPU: 91% (over the limit of 85%: no new servers right now)`).

## Strategies

Among the candidates the head picks one according to `strategy` in `cluster.yml` of the head:

| Strategy | Picks | Good for |
|----------|-------|----------|
| `least-memory` (default) | the node with the largest free memory budget, then fewer servers, then name | even distribution |
| `fill` | the node with the smallest free budget that is still enough, then fewer servers, then name | keeping machines free |
| `round-robin` | the next candidate in turn (sorted by name) | simple setups |

Without `maxMemoryMb` (0 = unlimited) all nodes have the same budget, so `least-memory` and `fill` effectively
choose the node with the fewest servers. Set `maxMemoryMb` on every node for memory-based placement.

## Start retries

If the chosen node cannot start the server (it rejects the request, the start fails, or it does not answer in
time), the head gives the ID back and tries **once** on the next best node:

```text
node-2 could not start Lobby-4: the template could not be synced within 7 s
Trying Lobby on node-3 instead (failed on node-2)...
```

If there is no other node or the retry fails too:

```text
Could not start a server of Lobby: it failed on node-2 and no other node can take it.
Could not start a server of Lobby: it failed on node-3 too.
```

A start the head gave up on is cancelled on the node, so a late start does not create an extra server
(with debug logging: `Not starting Lobby-4: the head cancelled the start`).

## Drain mode

A node in drain mode gets **no new servers**; its running servers keep running. Use it before maintenance or to
take a machine out of service.

```text
node drain node-2           # no new servers on node-2
node drain node-2 --stop    # also stop its non-static servers; they are replaced on other nodes
node undrain node-2
```

The commands work on every node (the head makes the change). The drain state travels with the heartbeats and is
stored on every node (`storage/internal/cluster/drain.json`), so it survives restarts and head changes. `node list`
marks draining nodes with `[DRAIN]`; a draining node logs on start
`This node is in drain mode: it gets no new servers until node undrain node-2.`

`--stop` does not stop static servers (they cannot move to another node).

HTTP: `POST /v1/nodes/{name}/drain` (optionally `?stop=true` or `{"stop": true}`) and
`POST /v1/nodes/{name}/undrain`, see [HTTP API](http-api.md).

## Stopping servers

`server stop`, `server stopall`, `server dispatch` and `server save` work for servers on any node; the request goes
to the node that runs the server (`Stopping Lobby-3 on node-2...`). Stopped servers may be replaced by
auto-scaling as usual.

Stopping a node (`exit`) stops the servers of **that node** only. The other nodes see it leave immediately and the
head replaces its servers elsewhere if `minServerCount` requires it.
