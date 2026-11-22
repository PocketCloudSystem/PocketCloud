# Upgrading from a single-node setup

A single installation keeps working after the update without any cluster configuration: it is one node that is
always head. The steps below first update it, then turn it into a cluster.

## 1. Before the update

- Back up the cloud folder (at least `storage/`, `templates/`, `groups/`).
- Stop the cloud cleanly (`exit`). This stops all servers.

## 2. Update the cloud

Replace `pocketcloud.jar` and start it. New on the first start:

- `storage/configs/node.yml` is created (`name: node-1`, `advertisedAddress: 127.0.0.1`, ...).
- `config.yml` gets the sections `redis` (disabled) and `cluster` (`strategy: least-memory`).
- `server_settings.yml` gets `addresses` and `bridgeReconnectTimeout`.
- Templates get the settings `nodes: []` and `staticNode: ""`.
- `storage/internal/auth-token` is created: the bridge token of the node is now persistent, so servers can
  reconnect after a restart of the cloud.
- `storage/internal/cluster/local-servers.json` records the running servers; after a crash or restart of the cloud
  the still running servers are adopted (see [Failures & recovery](failures.md#a-node-restarts-while-its-servers-keep-running)).

## 3. Update the bridge (required)

**The servers must run the new bridge plugin.** An old bridge

- shuts its server down as soon as the connection to the cloud is lost, so servers do not survive a restart of
  their node and cannot be adopted,
- does not receive the nodes (`CloudAPI.nodes()` is empty, `ICloudServer.node()` is `null`).

Each node copies the bridge from `storage/software/<software>/bridge/cloudbridge.jar` into every server folder
when the server is prepared (`plugins/cloudbridge.jar`). That jar is downloaded from
`https://github.com/PocketCloudSystem/PocketCloud/releases/download/latest-bridge/cloudbridge.jar` and, because
`bridge.checkForUpdates` is `true` in `storage/software/<software>.json`, replaced on startup whenever the file
size of the release differs.

- If the release at that URL already contains the cluster-capable bridge: delete
  `storage/software/<software>/bridge/` on every node and restart; it is downloaded again.
- If you build the bridge yourself (`target/cloud-bridge/compiled/cloudbridge.jar` after building the project):
  copy it to `storage/software/<software>/bridge/cloudbridge.jar` for **every** software (PowerNukkitX and
  WaterdogPE use the same jar) on **every** node, and set `"checkForUpdates": false` in the `bridge` section of
  `storage/software/<software>.json`, otherwise the next start replaces it with the release jar.

Then restart all servers once (e.g. `server stopall`; auto-scaling starts them again) so they pick up the new bridge.
Static servers get the new bridge on their next start too.

## 4. Prepare the cluster

Before adding the second node:

1. Set `name` and `advertisedAddress` in `node.yml` and restart (see [Setup](setup.md#1-prepare-the-first-node)).
2. Give every proxy template its nodes: `template nodes <proxy template> <node,...>`. Without it the proxy template
   is **disabled** as soon as the cloud runs as a cluster, and no proxy starts.
3. Give every static template its node: `template staticnode <template> <node>`. Their server folders stay on that
   node only.
4. Open the ports and check the addresses (see [Setup → Network](setup.md#network)).
5. Optionally set `maxMemoryMb` in `node.yml` and `cluster.strategy` in `config.yml`.

## 5. Add nodes

Follow [Setup](setup.md#3-add-the-second-node). With the `json` provider, the new nodes receive the templates,
groups, `templates.json` and `groups.json` from the head. With `mysql`, point every node to the same database.

Note: the first `node add` turns the cluster from 1 to 2 members. Until the second node has joined there is no
head, so no new servers are started (running servers keep running). Join the second node right away.

## Going back to a single node

Stop all nodes (`exit`), then on the node you keep delete `storage/internal/cluster/` and start it again. Without certificates the node
runs alone again. Remove the `nodes`/`staticNode` restrictions you no longer need; they are ignored on a single node.
