# PocketCloud Cluster

PocketCloud can run on several machines as one cluster. Every machine runs the same `pocketcloud.jar` as a
**node**; one node at a time is the **head** and decides where servers start. Servers (PowerNukkitX, WaterdogPE)
can run on any node and are managed from the console or HTTP API of any node.

A single machine without a cluster keeps working exactly as before: it is one node that is always head.

> **Status:** the cluster is part of PocketCloud V4 (branch `experimental/cluster`) and still experimental.

## Pages

| Page | Contents |
|------|----------|
| [Overview](overview.md) | Head-node model, what is shared, what each node does |
| [Setup](setup.md) | Requirements, ports, addresses, creating a cluster step by step |
| [Configuration](configuration.md) | `node.yml`, `config.yml` (`cluster`, `redis`), `server_settings.yml`, template settings `nodes`/`staticNode` |
| [Templates & replication](templates.md) | How template files are synchronised, `.pcignore`, conflicts, history, rollback, pause |
| [Scheduling](scheduling.md) | How the head picks a node, strategies, drain mode, start retries |
| [Failures & recovery](failures.md) | Node down, head down, network partition, adopting servers after a restart |
| [Security](security.md) | mTLS, the cluster CA, join tokens, removing nodes, certificates |
| [Commands](commands.md) | `node ...`, the cluster-related `template ...` subcommands, `server` in a cluster |
| [HTTP API](http-api.md) | Node routes, request forwarding, `X-PocketCloud-Node` |
| [Plugin API](plugin-api.md) | `INode`, `CloudAPI.nodes()`, `ICloudServer.node()`, `ServerSearchQuery.onNode` |
| [Upgrading](upgrading.md) | From a single-node setup to a cluster (bridge update!) |
| [Troubleshooting](troubleshooting.md) | Common log messages and what to do |
| [Known limitations](limitations.md) | What the cluster does not do (yet) |

## Quick start

```text
# machine 1 (existing PocketCloud, node.yml: name node-1, advertisedAddress 10.0.0.11)
node add node-2 10.0.0.12
node add node-3 10.0.0.13      # after node-2 has joined

# machine 2 and 3 (empty folder with pocketcloud.jar)
java -jar pocketcloud.jar --join <token file written by node add>

# any node
node list
```

Read [Setup](setup.md) before doing this on real machines: ports, addresses and proxy templates need attention.
