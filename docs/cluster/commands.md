# Commands

`<...>` is required, `[...]` is optional.

## Start parameter

| Parameter | Description |
|-----------|-------------|
| `java -jar pocketcloud.jar --join <token>` | Join a cluster with a token from `node add`. Needed only on the first start of the new node; the token is stored in `storage/internal/cluster/join-token` and applied (and deleted) during startup. |

## node

Aliases: `nodes`, `cluster`. All subcommands work on every node.

| Command | Description |
|---------|-------------|
| `node list` | All members with address, health (`ALIVE`/`SUSPECT`/`DEAD`), state, CPU, last heartbeat, plus head, term and majority. `[HEAD]` and `[DRAIN]` mark the head and draining nodes. On a single node it says that the node runs alone. |
| `node info <node>` | Address, drain mode, software the node can run, state, connection direction, term, version, uptime, number of servers, memory used by the cloud process, CPU against the 85 % limit. |
| `node add <node> <address> [port]` | Adds a member and prints its join token. `address` and `port` (default `3657`) become the `advertisedAddress` and `clusterPort` of the new node. The first `node add` on a single node creates the cluster. Only possible on the node that holds `ca.key`. Running it again for an existing name issues a new certificate and token. |
| `node join <token file or token>` | Joins a cluster from a running single node, without `--join` and without a restart. Stops the servers of this node first (the cluster's head decides afterwards what runs where). |
| `node remove <node>` | Removes a member; it is disconnected and can no longer connect. Not possible for the node you run it on. See [Security](security.md#removing-a-node). |
| `node drain <node> [--stop]` | The node gets no new servers. With `--stop` its non-static servers are also stopped (and replaced on other nodes). Processed by the head. |
| `node undrain <node>` | Ends drain mode. |

Example output of `node list`:

```text
Nodes (3) | head: node-1 | term: 4 | majority: 2
node-1 [HEAD] (this node) | 10.0.0.11:3657 | state: HEAD | cpu: 12%
node-2 [DRAIN] | 10.0.0.12:3657 | ALIVE | state: FOLLOWER | cpu: 8% | last seen: 0s ago
node-3 | 10.0.0.13:3657 | DEAD | state: FOLLOWER | cpu: 5% | last seen: 47s ago
```

## template (cluster subcommands)

| Command | Description |
|---------|-------------|
| `template nodes <template>` | Shows the nodes the template's servers may run on. |
| `template nodes <template> <node,node,...>` | Sets them. `all`, `-` or `[]` clears the list (all nodes). Warns about names that are not members (yet). Required for proxy templates. |
| `template staticnode <template>` | Shows the node the template's static servers live on. |
| `template staticnode <template> <node>` | Sets it; `none` or `-` clears it. Required for static templates in a cluster. Warns if the node is not in the template's `nodes`. |
| `template history <template>` | The last versions of a replicated folder (from the head's history, which also contains the versions recorded by former heads): version, time, node, note, changed/removed files, conflicts. |
| `template rollback <template> <version>` | Restores a version from the history as a new version and distributes it. |
| `template sync pause [template]` | Pauses the replication of one template (or all) **on this node** until `resume` (kept across restarts). |
| `template sync resume [template]` | Resumes it and sends what changed meanwhile. |
| `template sync status` | Version, hash, file count and sync state of every replicated folder on this node. |

`history`, `rollback` and `sync` take the replicated folder names: a template name (`Lobby`), `global/server`,
`global/proxy`, `groups/<name>` (or `group:<name>`), or `meta` for `templates.json`/`groups.json`. They report
`Template replication only runs while this node is part of a cluster.` on a single node.

`template info <template>` shows `Nodes` and `StaticNode`. `template create`, `edit` and `remove` work on every
node (see [Templates](templates.md#where-templates-are-edited-from-the-console)).

## server in a cluster

| Command | Behaviour in a cluster |
|---------|------------------------|
| `server start <template> [amount]` | Forwarded to the head, which picks the nodes. |
| `server stop <server\|template\|group\|type\|software> [-f]` | Each server is stopped by the node that runs it. |
| `server stopall [-f] [-y]` | Stops all servers of the cluster. |
| `server dispatch <server> <command>` | Executed by the node that runs the server; the answer comes back. |
| `server save <server>` | Executed by the node that runs the server. |
| `server list [filter]` | Shows a `Node` column. |
| `server info <server>` | Works for all servers and shows the `Node`. For a server on another node it shows the node and its address instead of `Path` and `Channel`. |
| `server screen <server>` | Live output of servers on **this** node. Remote servers: see [Coming soon](limitations.md#coming-soon). |

## exit

`exit` stops the servers of this node and leaves the cluster cleanly, so the other nodes see it as `DEAD` at once
and the head replaces its servers. Use `node drain <node> --stop` first if you want the replacements to be running
before the node goes down.
