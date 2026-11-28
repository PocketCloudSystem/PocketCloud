# Plugin API

The API (`cloud-api`) knows about nodes. It works the same in cloud plugins and in plugins on servers/proxies that
use the bridge. Without a cluster there is exactly one node.

## INode

`de.pocketcloud.api.cluster.INode`

| Method | Description |
|--------|-------------|
| `String name()` | Name of the node. |
| `String address()` | Address under which other nodes (and proxies) reach the node (`advertisedAddress`). |
| `int port()` | Cluster port of the node. |
| `String state()` | `HEAD`, `FOLLOWER`, `CANDIDATE`, `JOINING`, `ISOLATED` or `DEAD`. |
| `boolean head()` | Whether the node is the head. |
| `boolean alive()` | `!"DEAD".equals(state())` |

## CloudAPI.nodes()

`CloudAPI.instance().nodes()` returns an `INodeProvider`:

| Method | Description |
|--------|-------------|
| `Collection<INode> getAll()` | All members. |
| `Optional<INode> get(String name)` | One node. |
| `Optional<INode> head()` | The current head, if there is one. |

```java
INodeProvider nodes = CloudAPI.instance().nodes();

nodes.head().ifPresentOrElse(
        head -> logger.info("Head: " + head.name()),
        () -> logger.warn("There is no head right now"));

for (INode node : nodes.getAll()) {
    logger.info(node.name() + " " + node.address() + ":" + node.port() + " " + node.state());
}
```

On the cloud side the provider reads the live cluster state. On servers and proxies the bridge keeps the list it
received from its node with `SyncType.NODES`; the node sends it on connect and whenever a node joins, leaves, dies
or the head changes (checked every 2 s). Plugins there have no node events; check the provider when you need the
current state. If the bridge is connected to an older cloud that does not send nodes, `getAll()` is empty.

## Node events

Cloud plugins (in `storage/plugins/` of a node) get these events, listened to like the other cloud events
(package `de.pocketcloud.cloud.event.impl.node`). Each node fires them for what it sees itself:

| Event | When | Getters |
|-------|------|---------|
| `NodeConnectedEvent` | Another node connected (also after a reconnect). | `getNode()` |
| `NodeDisconnectedEvent` | Another node left or is considered dead (10 s without heartbeat); its servers are treated as offline. | `getNode()` |
| `HeadChangedEvent` | A node became head. | `getHead()`, `getPreviousHead()` (null if none was known), `isThisNode()` |

## Discord alerts

With `discordWebhook.notifications.node-alerts: true` in `log_settings.yml` (and the webhook enabled) the cluster
sends Discord messages when a node goes offline or comes back, when the head changes, and when a node certificate
or the cluster CA expires soon. The head sends them (while there is no head, the oldest reachable node), so every
change arrives once; certificate warnings come from the affected node. Configure the same webhook on all nodes.

## ICloudServer.node()

```java
ICloudServer server = CloudAPI.instance().servers().get("Lobby-3").orElseThrow();
INode node = server.node();   // null if unknown (older cloud)
if (node != null && !node.alive()) {
    // the server's node is gone; the server will be treated as offline
}
```

If the bridge has a node name for a server but not (yet) the node itself, `node()` returns a node with that name,
no address and state `UNKNOWN`.

## ServerSearchQuery.onNode

```java
Collection<ICloudServer> lobbiesOnNode2 = CloudAPI.instance().servers().query(
        ServerSearchQuery.create().ofTemplate(lobbyTemplate).onNode("node-2"));

// or
CloudAPI.instance().servers().query(q -> q.onNode("node-2"));
```

The same filter is available in `QUERY /v1/servers` as `nodeName` (see [HTTP API](http-api.md#server-routes)).

## SyncType.NODES

`de.pocketcloud.shared.sync.SyncType.NODES` is the sync type the node uses to send all nodes to its bridges. Its
payload is a list of `BaseNode` (`cloud-shared`, fields `name`, `address`, `port`, `state`, `head`). Plugins
normally use `CloudAPI.nodes()` instead of handling the sync packet.

## Behaviour of existing API calls in a cluster

- `servers().start(template, count)` is executed by the head, wherever it is called.
- `stop`, `dispatch`, `save` of an `ICloudServer` are executed by the node that runs the server.
- Templates, groups and server lists contain the whole cluster on every node.
