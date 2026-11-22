# Security

## Mutual TLS between nodes

The cluster port (`3657`) only speaks TLS, and **both sides** present a certificate (mutual TLS):

- The first `node add` creates a private **cluster CA** (`CN=PocketCloud Cluster CA <cloudName>`, EC P-256,
  valid 10 years) on that node.
- `node add <name> <address>` issues a certificate for the new node with its name as CN (EC P-256, valid 825 days)
  and prints a **join token**.
- A connection is only accepted if the peer's certificate was signed by the cluster CA. After the handshake the
  first packet must be a hello whose node name equals the certificate's CN, and the name must be in the member list.
- Nodes with a different cluster protocol version are rejected.

Rejected connections are logged (at most once a minute per node/host):

```text
Could not connect securely with 203.0.113.7. Is its certificate from this cluster and still valid?
Refused the connection of node node-4 (10.0.0.14:51234): not a member of this cluster
The node at 10.0.0.14:3657 refused the connection: name does not match certificate
```

Possible reasons in the rejection: `protocol version X != 1`, `name does not match certificate`,
`same name as this node`, `not a member of this cluster`.

The bridge port (`3656`) stays on `127.0.0.1` and does not use TLS; bridges authenticate with the node's auth token
(`storage/internal/auth-token`).

The **game server ports are not protected** by the cluster. Restrict them to the node IPs with a firewall or keep
them in a private network.

## Files

In `storage/internal/cluster/`:

| File | On | Secret? |
|------|----|---------|
| `ca.crt` | all nodes | no |
| `ca.key` | **only the node that created the cluster** | **yes** - whoever has it can create node certificates |
| `node.crt` | each node | no |
| `node.key` | each node | **yes** |

Private keys are written with owner-only file permissions where the file system supports it.

**Back up `ca.key`.** Only the node that holds it can run `node add`
(`Only the node holding the cluster CA key (ca.key) can add nodes.`). If it is lost, existing nodes keep working,
but no node can be added until you create a new cluster CA (which means re-joining every node).

## Join tokens

A token looks like `pcjoin1.<base64url>@<address>:<port>`. It contains:

- the node name,
- the CA certificate,
- the node certificate **and its private key**,
- the seeds (addresses of the existing members),
- after the `@`: the address and cluster port of the new node.

Treat a token like a password:

- `node add` writes it to `storage/internal/cluster/join-tokens/<node>.token` (permissions `rw-------`) and never
  prints it. Transfer the file over a secure channel, use it once (`java -jar pocketcloud.jar --join <file>`) and
  delete it on both machines afterwards.
- Versions before this change printed the token to the console log. Delete old `storage/logs/` files that contain
  `pcjoin1.` if other people can read them.
- A token cannot be revoked by itself. If it leaked before it was used, run `node remove <name>` and add the node
  again (see the limitation below).

Running `node add` again for an existing name issues a new certificate and token for it, e.g. after reinstalling a
machine.

## Removing a node

```text
node remove node-4
```

removes the node from the member list. The change spreads to all nodes within a second, and the removed node is
disconnected and rejected from then on (`not a member of this cluster`). Every node treats it like a failed node:
its servers and players are written off (`Node node-4 has disconnected. Its servers are offline: ...`) and the
head starts replacements. This is the revocation mechanism: only
members may connect.

Recommended order:

1. `node drain node-4 --stop` and wait until its servers have been replaced,
2. stop the node (`exit`),
3. `node remove node-4`.

Removing a node lowers the number of members and therefore the majority. A node cannot remove itself
(`A node cannot remove itself.`).

> **Limitation:** there is no certificate revocation list. The certificate of a removed node stays technically
> valid until it expires; it is rejected only because the name is no longer a member. If you add a node with the
> **same name** again later, the old certificate (and an old leaked token) would be accepted again. Use a new name
> when the old key may be compromised.

## Certificate lifetime

Node certificates are valid for **825 days**, the CA for 10 years. A node warns on the console 30 days before its
certificate expires (90 days for the CA). If the head holds `ca.key`, it renews certificates automatically within
those 30 days (`Renewed the certificate of node node-2.`); otherwise it warns and names the nodes that can renew.
You can also renew by hand with `node cert renew [node]` and check the validity with `node cert info`
(see [Commands](commands.md)). The private key never leaves the renewed node.

A node with an **expired** certificate can no longer connect and cannot be renewed this way: run
`node add <same name> <address>` on the CA node and start that node once with the new token.

## HTTP API

Each node checks the `Authorization: Bearer <auth-key>` header against **its own** `httpServer.auth-key`. Requests
that a node forwards to the head or to another node travel over the mutually authenticated cluster connection and
are not checked again there. Use the same `auth-key` on all nodes if clients may connect to any of them.
