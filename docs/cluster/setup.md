# Setting up a cluster

## Requirements

- **Java 25** on every machine.
- **The same PocketCloud version on every node.** Nodes with a different cluster protocol version are rejected
  (`protocol version X != 1`).
- **Clocks in sync** (NTP). The election compares the start times of the nodes and the member list uses
  timestamps as version numbers.
- **3 or more nodes** if the cluster should keep working when one machine fails. With 2 nodes the majority is 2:
  if either node is down there is no head, and no new servers are started until it is back. See
  [Overview](overview.md#the-head-node-model).
- Redis is **optional**. The cluster works without it; see [Configuration](configuration.md#redis).

## Network

| Port | Protocol | Between | Purpose |
|------|----------|---------|---------|
| `3657` (`node.yml` → `clusterPort`) | TCP, TLS | all nodes, both directions | Node-to-node connections (mutual TLS). Every node connects to every other node. |
| Server port range (`server_settings.yml` → `serverPortRanges.server`, default `40000`-`65535`) | UDP **and TCP** | all nodes, both directions | Proxies on one node connect to game servers on another node. WaterdogPE uses NetherNet: HTTP signaling over TCP on the server port, then UDP. Narrow the range (e.g. `40000`-`40100`) so only a few ports must be opened. |
| Proxy port range (`serverPortRanges.proxy`, default `19132`-`20000`) | UDP | players → proxy nodes | Player connections, only on the nodes that run proxies. |
| `3656` (`config.yml` → `network.port`) | TCP | local only | Bridge connections. Keep `network.address: 127.0.0.1`: bridges always talk to the node on their own machine. |
| `8080` (`config.yml` → `httpServer.port`) | TCP | clients → node | HTTP API, only if enabled. Every node can answer. |

Keep the cluster port and the game server ports inside a private network, or restrict them with a firewall to the
IPs of your nodes. The cluster port is protected by mutual TLS (see [Security](security.md)), the game server ports
are not.

## Addresses

Each node has three address settings that matter in a cluster:

| Setting | File | Meaning |
|---------|------|---------|
| `advertisedAddress` | `node.yml` | How **other nodes** reach this node: cluster port **and** game servers. Must be an IP or host name that every other node can reach. Never `127.0.0.1` when the nodes are on different machines. |
| `clusterBindAddress` | `node.yml` | Where the cluster port listens. Default `0.0.0.0`. |
| `addresses.public-address` | `server_settings.yml` | The address sent to proxies for servers of this node. **In a cluster, a loopback value (`127.0.0.1`, the default) is replaced by `advertisedAddress` automatically.** Set it only if proxies must use a different address than the other nodes. |
| `addresses.server-bind-address` / `addresses.proxy-bind-address` | `server_settings.yml` | What the game servers / proxies listen on. **In a cluster, a loopback server bind address is widened to `0.0.0.0`** (with a warning) when the node is advertised under a non-loopback address. Set the private IP of the machine here to avoid listening on all interfaces. |

If the bind address is widened you see once:

```text
The bind address 127.0.0.1 is not reachable from other nodes, servers bind to 0.0.0.0 instead. Set addresses.server-bind-address in server_settings.yml to choose an address.
```

All nodes on **one** machine (for testing) can keep `127.0.0.1` everywhere; then nothing is rewritten. They need
different `clusterPort`s, bridge ports (`network.port`) and folders.

## Step by step

The example uses three machines in a private network:

| Node | Machine IP |
|------|------------|
| `node-1` | `10.0.0.11` (existing PocketCloud installation) |
| `node-2` | `10.0.0.12` (new) |
| `node-3` | `10.0.0.13` (new) |

### 1. Prepare the first node

On the existing installation (or a fresh one, started once so that the files exist), stop the cloud and edit
`storage/configs/node.yml`:

```yaml
name: node-1
advertisedAddress: 10.0.0.11
clusterBindAddress: 0.0.0.0
clusterPort: 3657
maxMemoryMb: 16384   # memory for servers on this machine, 0 = no limit
seeds: []
```

Upgrading from an older version? Read [Upgrading](upgrading.md) first: **the bridge jar must be updated**.

Start the cloud again. `node list` shows:

```text
This node (node-1) runs alone. Use node add <name> <address> [port] to create a cluster.
```

### 2. Prepare the templates

In a cluster some templates need to know where they may run. Do this before adding nodes:

- **Proxy templates must name their nodes**, e.g. only the machines with a public IP. Without `nodes` a proxy
  template is disabled in the cluster.

  ```text
  template nodes Proxy node-1
  ```

- **Static templates** (`staticServers: true`) must name the one node their static servers live on:

  ```text
  template staticnode SkyBlock node-1
  ```

- Optional: restrict other templates with `template nodes <template> <node,node,...>`.

See [Configuration](configuration.md#template-settings) and [Scheduling](scheduling.md).

### 3. Add the second node

On `node-1`:

```text
node add node-2 10.0.0.12
```

Syntax: `node add <node> <address> [port]`, port defaults to `3657`. The address is the **`advertisedAddress` of
the new node** and is written into its `node.yml` when it joins.

The first `node add` creates the cluster CA (`Creating a new cluster...`) and opens the cluster port. The output:

```text
Creating a new cluster...
Cluster connection has been established on 0.0.0.0:3657 (node node-1).
Node node-2 has been added.
Join token: /srv/pocketcloud/storage/internal/cluster/join-tokens/node-2.token (contains a private key, keep it secret)
Copy it to the new node, start that node once with java -jar pocketcloud.jar --join <path to node-2.token>, then delete the file.
No new servers are started until node-2 has joined (with 2 nodes, both must be online).
```

From now on `node-1` waits for `node-2`
(`Only 1/2 nodes are reachable. This node is no longer the head until a majority is back.`). Running servers keep
running. Continue with the next step right away.

> The token contains the private key of the new node. It is never printed to the console or the log files, only
> written to the token file (readable by the owner only). See [Security](security.md#join-tokens).

### 4. Join with the second node

Copy `node-2.token` to machine 2 (e.g. with `scp`), put `pocketcloud.jar` into an empty folder and start it
**once** with the token file (the token text itself is accepted as well):

```bash
java -jar pocketcloud.jar --join /path/to/node-2.token
```

Alternatives without `--join`:

- On a node that is already running (single node): `node join /path/to/node-2.token` in its console. Its own
  servers are stopped first, then it connects right away.
- Before starting: put the token text into `storage/internal/cluster/join-token` of the new node and start it
  normally; the file is applied and deleted on start.

Later starts never need `--join` again.

The node stores the certificates in `storage/internal/cluster/`, writes `name`, `seeds`, `advertisedAddress` and
`clusterPort` into its `node.yml` and logs:

```text
Joined the cluster as node-2.
Cluster connection has been established on 0.0.0.0:3657 (node node-2).
Node node-1 has connected.
node-1 is now the head.
```

Then it fetches all templates from the head (`Template Lobby updated to version 1 (12 file(s) fetched, 3400 KB).`)
and, with the `json` provider, the template and group definitions
(`Template Lobby was created on another node and is now available here.`).

Later starts need no token: `java -jar pocketcloud.jar`.

Like any installation, the new node downloads the server software and the bridge itself on its first start. A node
without the software of a template gets no servers of it (see [Scheduling](scheduling.md)). Check `node.yml` →
`maxMemoryMb` on the new node too.

### 5. Add the third node

Back on `node-1` (only the node that created the CA can add nodes):

```text
node add node-3 10.0.0.13
```

and on machine 3:

```bash
java -jar pocketcloud.jar --join '<token>'
```

With 3 members the majority is 2, so the cluster keeps its head while `node-3` joins.

### 6. Verify

On any node:

```text
node list
```

```text
Nodes (3) | head: node-1 | term: 1 | majority: 2
node-1 [HEAD] (this node) | 10.0.0.11:3657 | state: HEAD | cpu: 12%
node-2 | 10.0.0.12:3657 | ALIVE | state: FOLLOWER | cpu: 8% | last seen: 0s ago
node-3 | 10.0.0.13:3657 | ALIVE | state: FOLLOWER | cpu: 5% | last seen: 0s ago
```

All nodes should be `ALIVE` and show the same head and term. Then check:

- `node info node-2` shows the software the node can run.
- `server list` shows a `Node` column; start a server with `server start Lobby 3` and see it spread over the nodes.
- `template sync status` shows every template `in sync` on the followers.
- Join through a proxy and switch to a lobby on another node.

## Running all nodes on one machine (testing)

Use one folder per node, give each its own `name`, `clusterPort`, bridge port (`config.yml` → `network.port`)
and, if enabled, HTTP port, and keep `advertisedAddress: 127.0.0.1`. Use `node add node-2 127.0.0.1 3658` etc.
The nodes share the machine's ports; the free-port check of each node sees ports already used by the others.
