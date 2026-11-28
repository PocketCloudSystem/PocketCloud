# Configuration reference

All files are in `storage/configs/` of each node. Restart the node after changing them.

## node.yml

Settings of this node. Created on the first start; `--join` fills in `name`, `seeds`, `advertisedAddress` and
`clusterPort`.

| Key | Default | Description |
|-----|---------|-------------|
| `name` | `node-1` | Unique name of the node in the cluster. Letters, digits, `-` and `_`, at most 32 characters. Must match the name in the node certificate, otherwise: `The name X in node.yml does not match the certificate of this node (Y). Running as a single node.` |
| `advertisedAddress` | `127.0.0.1` | Address other nodes use to reach this node: the cluster port **and** its game servers. Never `0.0.0.0`. If you change it (or `clusterPort`) later, restart the node: the head corrects its entry in the member list and spreads it (`Member address of node-2 updated: ...`). See [Setup → Addresses](setup.md#addresses). |
| `clusterBindAddress` | `0.0.0.0` | Address the cluster port binds to. |
| `clusterPort` | `3657` | Port for node-to-node connections (TLS). |
| `maxMemoryMb` | `0` | Memory in MB this node may use for servers. `0` = no limit. The head only places a server on a node whose budget minus the `maxMemory` of the servers already running there is at least the template's `maxMemory`. |
| `seeds` | `[]` | Nodes to contact when joining (`host:port`). Filled automatically by `--join`. A node with no seeds and no member list creates the member list itself. |

Without cluster certificates in `storage/internal/cluster/` the node ignores everything but `name` and runs alone
as head (single-node mode).

## config.yml and its neighbours

The settings of the cloud are split over several files in `storage/configs/`:

| File | Content |
|------|---------|
| `config.yml` | `cloudName`, `language`, `provider`, update checks, `paths`, `network` (bridge port) |
| `cluster.yml` | Settings for several nodes: `strategy`, `templateStorage`, `restartOnUpdate`, `s3` |
| `redis.yml` | Redis connection |
| `mysql.yml` | MySQL connection (only with `provider: mysql`) |
| `http.yml` | HTTP API: `enabled`, `address`, `port`, `auth-key`, `ssl`, `routePrefix` |
| `backups.yml` | Automatic backups |
| `log_settings.yml`, `server_settings.yml`, `node.yml` | as before |

An older `config.yml` that still has these sections is split on the first start; the values are kept.

### backups.yml

```yaml
intervalHours: 0
keep: 7
uploadToS3: false
```

| Key | Default | Description |
|-----|---------|-------------|
| `intervalHours` | `0` | Creates `storage/backups/auto-<time>.zip` every X hours (`0` = off), like `backup create`. In a cluster only the head does it, checked every 10 minutes. |
| `keep` | `7` | Number of automatic backups that are kept; older ones are deleted. Manual and `before-restore` backups are never deleted. |
| `uploadToS3` | `false` | Also uploads every backup to the bucket of `s3` in `cluster.yml` (`<prefix>backups/<file>`), with the credentials of the [S3 storage](templates.md#s3-storage). Restoring from S3 means downloading the file into `storage/backups/` first. |

### paths

```yaml
paths:
  software: storage/software/
  inGame: storage/inGame/
  libraries: storage/libraries/
  plugins: storage/plugins/
```

| Key | Default | Description |
|-----|---------|-------------|
| `paths.software` | `storage/software/` | Server software (jars, binaries, default configs, bridge). |
| `paths.inGame` | `storage/inGame/` | In-game files (languages, `whitelist.json`, `notificationList.json`). |
| `paths.libraries` | `storage/libraries/` | Libraries (`libraries.json` and the extracted libraries). |
| `paths.plugins` | `storage/plugins/` | Cloud plugins and their data folders. |

Relative paths start in the cloud folder, absolute paths (e.g. `/srv/pocketcloud/software/`) can point anywhere.
Missing folders are created at startup. Changes need a restart; existing files are not moved, so move the folder
yourself before you change the path. The paths are per node: in a cluster every node reads its own `config.yml`.
Several nodes on one machine can share the software folder; do not share `inGame` between nodes with the `json`
provider, because each node writes its own whitelist and notification list there.

### cluster.yml

```yaml
strategy: least-memory
templateStorage: packets
restartOnUpdate: []
s3:
  endpoint: https://s3.eu-central-1.amazonaws.com
  region: eu-central-1
  bucket: pocketcloud
  prefix: templates/
```

| Key | Default | Values |
|-----|---------|--------|
| `strategy` | `least-memory` | How the head picks the node for a new server (only used with more than one node). `least-memory`: the node with the most free memory budget (even distribution). `fill`: the node with the smallest free budget that is still enough (keeps machines free). `round-robin`: the next node in turn (by name). Invalid values are reset to `least-memory`. Only the head's setting is used, so keep it the same on all nodes. |
| `restartOnUpdate` | `[]` | Templates whose servers get a rolling restart (like `template restart`) after the template was changed, e.g. `[Lobby, BedWars]`. Only the head's setting is used. A change during a running restart restarts the template once more afterwards. |
| `templateStorage` | `packets` | Where the nodes get template files from. `packets`: from the head over the cluster connection. `s3`: from an S3 bucket, see [Templates → S3 storage](templates.md#s3-storage). |
| `s3.endpoint` | `https://s3.eu-central-1.amazonaws.com` | URL of the S3 service, e.g. `http://10.0.0.5:9000` (MinIO) or `https://<account>.r2.cloudflarestorage.com` (Cloudflare R2). |
| `s3.region` | `eu-central-1` | Region of the bucket. MinIO: `us-east-1`, R2: `auto`. |
| `s3.bucket` | `pocketcloud` | The bucket. It must exist. |
| `s3.prefix` | `templates/` | Folder inside the bucket. Use the same bucket and prefix on all nodes. |

### redis.yml

```yaml
enabled: false
address: 127.0.0.1
port: 6379
username: ''
password: ''
database: 0
ssl: false
timeout: 3000
```

| Key | Default | Description |
|-----|---------|-------------|
| `enabled` | `false` | Use Redis for the shared cluster state and as a cache in front of the provider. |
| `address`, `port` | `127.0.0.1`, `6379` | Redis server. |
| `username`, `password` | empty | Leave empty when ACLs / no password are used. |
| `database` | `0` | Database index. |
| `ssl` | `false` | TLS to Redis. |
| `timeout` | `3000` | Command timeout in ms (values below 100 are reset to 3000). |

<a id="redis"></a>
**Is Redis required?** No. Nodes exchange servers, players, templates and drain state directly over the cluster
port, and a cluster runs without Redis. If you enable it:

- **all nodes must use the same Redis and the same `cloudName`**: keys are prefixed with `pc:<cloudName>:`
  (`pc:<cloudName>:term`, `:head`, `:servers`, `:players`, `:ids:<template>`, `:node:<name>:servers`);
- a new head writes its term to Redis with a check that no newer term of **this cluster** is stored there (fencing).
  The term is stored with the cluster id (serial of the cluster CA); a term of another cluster with the same
  `cloudName` is replaced, and a newer term of this cluster that no member reports is skipped (see
  [Troubleshooting](troubleshooting.md#redis)). **While Redis is unreachable no node can become head**
  (`This node cannot become the head while Redis is unreachable: ...`);
- the provider (json/mysql) stays the persistent store; Redis caches templates, groups, whitelist and notifications
  in front of it;
- on start a node removes the servers and players it left in Redis after a crash
  (with debug logging: `Removed N stale server(s) and M player(s) of this node from the shared state`).

If Redis cannot be reached at startup the node logs `Could not connect to Redis: ...` and
`Running without Redis. This only works with a single node.`

### Other keys that matter in a cluster

| Key | Notes |
|-----|-------|
| `cloudName` | Used as the Redis namespace and in the name of the cluster CA. Use the same value on all nodes. |
| `provider` | `json`: `templates/templates.json` and `groups/groups.json` are replicated between nodes. `mysql`: all nodes use the same database. Use the same provider on all nodes. |
| `network.address`, `network.port` | Bridge port. Keep `127.0.0.1`; bridges only talk to the node on their machine. |
| `http.yml` | Each node runs its own HTTP server with its own `auth-key`. Use the same key on all nodes if clients may talk to any node. |

## server_settings.yml

```yaml
addresses:
  public-address: 127.0.0.1
  server-bind-address: 127.0.0.1
  proxy-bind-address: 0.0.0.0
bridgeReconnectTimeout: 60
```

| Key | Default | Description |
|-----|---------|-------------|
| `addresses.public-address` | `127.0.0.1` | Address sent to proxies when a server of this node is registered. In a cluster a loopback value is replaced by the node's `advertisedAddress`. |
| `addresses.server-bind-address` | `127.0.0.1` | Address sub-servers (PowerNukkitX) bind to. In a cluster a loopback value is widened to `0.0.0.0` if the node's `advertisedAddress` is not loopback (logged once). Set the machine's private IP to listen on one interface only. |
| `addresses.proxy-bind-address` | `0.0.0.0` | Address proxies (WaterdogPE) bind to. |
| `bridgeReconnectTimeout` | `60` | Seconds a server keeps trying to reconnect to its node after losing the connection (e.g. a node restart) before it shuts down. Used at least 10 s. Also controls how long a restarted node waits for the head's decision about servers of its last run (this value + 30 s). Passed to the bridge as `network-reconnect-timeout` when a server starts. |

`serverPortRanges` is unchanged, but remember that the server range must be reachable (UDP) between the nodes.

## Template settings

Two settings were added to every template (with the `json` provider in `templates/templates.json` → `settings`;
existing templates get them on the next start):

| Key | Default | Description |
|-----|---------|-------------|
| `nodes` | `[]` | Nodes servers of this template may run on. Empty = all nodes. **Required for proxy templates** in a cluster; a proxy template without `nodes` is disabled. |
| `staticNode` | `""` | The node the static servers of this template live on. **Required when `staticServers` is true** in a cluster; otherwise the template is disabled. Static servers never start on another node, and their folders are not replicated. The static node must also be allowed by `nodes`. |

Both are ignored on a single node. Set them with:

```text
template nodes <template>                       # show
template nodes <template> <node,node,...|all>   # set ("all", "-" or "[]" clears)
template staticnode <template>                  # show
template staticnode <template> <node|none>      # set ("none" or "-" clears)
```

or with `PATCH /v1/templates/{name}` and a body like `{"nodes": ["node-1", "node-2"], "staticNode": ""}`.
`template info <template>` shows both. On load the cloud warns about templates that are disabled because of them:

```text
Proxies of Proxy cannot start until you choose their nodes with template nodes Proxy <node,...>.
Static servers of SkyBlock cannot start until you choose their node with template staticnode SkyBlock <node>.
```

## Files in storage/internal

| Path | Content |
|------|---------|
| `storage/internal/auth-token` | Bridge auth token of this node. Persistent, so servers can reconnect after a node restart. |
| `storage/internal/cluster/ca.crt` | Cluster CA certificate (all nodes). |
| `storage/internal/cluster/ca.key` | Cluster CA private key. **Only on the node that created the cluster.** Needed for `node add`. |
| `storage/internal/cluster/node.crt`, `node.key` | Certificate and key of this node. |
| `storage/internal/cluster/members.json` | Member list (with version). |
| `storage/internal/cluster/term.json` | Highest known head term and the term this node last voted in. |
| `storage/internal/cluster/drain.json` | Nodes in drain mode. |
| `storage/internal/cluster/local-servers.json` | Servers this node runs (name, uuid, template, id, port, pid, ...), used to adopt them after a restart. Also used in single-node mode. |
| `storage/internal/cluster/template-sync/` | Template versions known to this node (`applied.json`) and, on a head, the versions it serves (`head.json`); temp folders. |
| `storage/internal/cluster/template-history/` | Template history (last 10 versions per template): recorded by the head, copied by the followers together with the files of the versions they applied. |
| `storage/internal/cluster/template-sync/paused.json` | Templates whose sync is paused on this node (`template sync pause`). |
| `storage/internal/cluster/join-token` | Exists only between `--join` and the next start. |
