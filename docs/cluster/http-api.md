# HTTP API in a cluster

The HTTP API runs on **every node** that has `enabled: true` in `http.yml`. A client can talk to any node:

- **Reads** are answered by that node from its cache, which contains the whole cluster.
- **Writes with cluster effect** are forwarded to the **head**.
- **Requests about a server on another node** (logs, command, save, stop) are forwarded to **the node that runs
  it**.

Authentication happens on the node the client talks to, with that node's `auth-key` (`http.yml`) (see
[Security](security.md#http-api)). Paths below are shown without the route prefix (`/cloud/` by default), as in
the [HTTP API documentation](../../http_api_docs.md).

## X-PocketCloud-Node

Every response carries the header

```http
X-PocketCloud-Node: node-2
```

with the name of the node that **processed** the request: the node itself, or the head / owning node if the request
was forwarded.

## Forwarding rules

| Request | Processed by |
|---------|--------------|
| `GET /v1/servers/{name}/logs`, `POST /v1/servers/{name}/dispatch`, `POST /v1/servers/{name}/save`, `POST /v1/servers/{name}/stop` for a server on another node | the node that runs the server |
| `POST /v1/servers/start` | the head |
| Any non-reading method (`POST`, `PUT`, `PATCH`, `DELETE`) on `/v1/templates...`, `/v1/groups...`, `/v1/maintenance...` | the head |
| `POST /v1/nodes/{name}/drain`, `POST /v1/nodes/{name}/undrain` | the head |
| `GET /v1/templates/{name}/history` | the head |
| Everything else (all `GET`/`HEAD`/`OPTIONS`/`QUERY` requests, players, plugins, notifications, `stopAll`, ...) | the node the client talks to |

Forwarded requests time out after 8 s. Errors produced by forwarding:

| Status | Message |
|--------|---------|
| `503` | `There is no head node right now.` (no majority, or during an election) |
| `503` | `Node <name> is not reachable.` |
| `504` | `Node <name> did not answer: ...` |
| `502` | `Unexpected answer from node <name>.` |

With debug logging the receiving node logs `Handled HTTP POST /v1/servers/start forwarded by node node-2 (200)`.

## Server routes

Server objects in `GET /v1/servers`, `QUERY /v1/servers` and `GET /v1/servers/{name}` contain a `node` field with
the name of the node the server runs on.

`QUERY /v1/servers` accepts `nodeName` to filter by node:

```http
QUERY /v1/servers HTTP/1.1
Authorization: Bearer your-secret-token
Content-Type: application/json

{"nodeName": "node-2"}
```

## Node routes

These routes exist on single nodes too (with exactly one node and `clustered: false`).

### `GET /v1/nodes`

The nodes as seen by the node that answers.

```json
{
  "clustered": true,
  "head": "node-1",
  "term": 4,
  "nodes": [
    {
      "name": "node-1",
      "local": true,
      "head": true,
      "address": "10.0.0.11",
      "port": 3657,
      "drain": false,
      "softwares": ["powernukkitx-latest", "waterdogpe-latest"],
      "cpuLimit": 85.0,
      "servers": 3,
      "usedMemoryMb": 1536,
      "state": "HEAD",
      "health": "ALIVE",
      "version": "1.0.0",
      "maxMemoryMb": 16384,
      "startedAt": 1795430000000,
      "cpu": 12.5
    },
    {
      "name": "node-2",
      "local": false,
      "head": false,
      "address": "10.0.0.12",
      "port": 3657,
      "drain": false,
      "softwares": ["powernukkitx-latest"],
      "cpuLimit": 85.0,
      "servers": 2,
      "usedMemoryMb": 1024,
      "state": "FOLLOWER",
      "health": "ALIVE",
      "connected": true,
      "lastSeen": 1795430123456,
      "version": "1.0.0",
      "startedAt": 1795429000000,
      "maxMemoryMb": 8192,
      "cpu": 8.1
    }
  ]
}
```

| Field | Description |
|-------|-------------|
| `local` | Whether this is the node that answered. |
| `head` | Whether the node is head. |
| `state` | `JOINING`, `FOLLOWER`, `CANDIDATE`, `HEAD`, `ISOLATED`; `UNKNOWN` for a member this node never connected to. |
| `health` | `ALIVE`, `SUSPECT` or `DEAD`. |
| `drain` | Drain mode. |
| `softwares` | Server software the node can run (missing if not reported). |
| `servers`, `usedMemoryMb` | Number of servers on the node and the sum of their templates' `maxMemory`. |
| `maxMemoryMb` | Memory budget from the node's `node.yml` (0 = unlimited). |
| `cpu`, `cpuLimit` | CPU load in percent and the limit above which the node gets no new servers. |
| `connected`, `lastSeen` | Remote nodes only: connection state and time of the last heartbeat (ms). |
| `version`, `startedAt` | PocketCloud version and start time (ms) of the node. |

### `GET /v1/nodes/{name}`

One node, same fields plus `serverNames` (the servers on that node). `404` if the node is not a member.

### `POST /v1/nodes/{name}/drain`

Puts the node into drain mode (no new servers). With `?stop=true` or the body `{"stop": true}` its non-static
servers are also stopped and replaced on other nodes. Processed by the head. Returns the node object plus a
`message`.

`404` if the node is not a member, `409` with the reason if the change failed (e.g. `There is no head right now.`)
or if the cloud does not run as a cluster (`Drain mode only exists in a cluster.`).

### `POST /v1/nodes/{name}/undrain`

Ends drain mode. Same responses as `drain`.

## Template routes

`POST /v1/templates` and `PATCH /v1/templates/{name}` accept the template settings `nodes` (list of node names) and
`staticNode` (string), see [Configuration](configuration.md#template-settings).

`POST /v1/templates` also takes a short form without a `settings` object: `name`, `templateType` (`SERVER`/`PROXY`),
`serverSoftware`, plus any flat setting of `PATCH`. Missing settings get their defaults. Invalid values answer `400`.

```json
{"name": "Lobby", "templateType": "SERVER", "serverSoftware": "powernukkitx-latest", "minServerCount": 2}
```

### `GET /v1/templates/{name}/history`

The file versions of a template, newest first (see [Templates & replication](templates.md)). Answered by the head.
`409` without a cluster.

```json
{"currentVersion": 12, "records": [
  {"version": 12, "time": 1795869278000, "node": "node-1", "note": "rollback to version 10", "deleted": false,
   "changed": ["junk.txt"], "removed": [], "conflicts": []}
]}
```

### `POST /v1/templates/{name}/rollback`

Body `{"version": 10}`. Like `template rollback`: the head distributes that version as a new version. `404` if the
version does not exist.

### `POST /v1/templates/{name}/restart`

Rolling restart like `template restart`. `409` if one is already running for the template.

## Schedules

Scheduled console commands, like the `schedule` command. Replicated in a cluster; the head runs them.

| Request | Body / answer |
|---------|---------------|
| `GET /v1/schedules` | `[{"id": "3395e1", "cron": "0 4 * * *", "command": "template restart Lobby"}]` |
| `POST /v1/schedules` | `{"cron": "0 4 * * *", "command": "template restart Lobby"}`; `400` for an invalid cron time |
| `DELETE /v1/schedules/{id}` | `404` if there is no such schedule |

## Backups

The backups of the node that answers, like the `backup` command. The zip files cannot be downloaded over HTTP because
they contain secrets.

| Request | Body / answer |
|---------|---------------|
| `GET /v1/backups` | `[{"name": "backup-2026-11-28_12-34-27.zip", "time": 1795869267381, "size": 3012072}]`, newest first |
| `POST /v1/backups` | Creates a backup and answers when it is done: `{"message": ..., "name": ...}` |
| `POST /v1/backups/{name}/restore` | Restores; answers with `before`, the backup of the previous state |

## Stats history

`GET /v1/stats/history` returns one point per minute for the last 24 hours, recorded in memory by the node that
answers (empty after a restart). Each point is `[time, players, servers, cpuPercent, memoryBytes]`; CPU and memory
are of that node's cloud process.

```json
{"interval_ms": 60000, "points": [[1795869600000, 12, 4, 2.5, 83886080]]}
```

`GET /v1/groups` entries contain `templates`, and `GET /v1/plugins` entries contain `enabled`.

## Web panel routes

`/panel/` serves the [web panel](../web-panel.md). `/panel-api/...` (login, logout, session, live console as
Server-Sent Events, software list) is meant for the panel only and may change without notice.

## Metrics (Prometheus)

### `GET /metrics`

Metrics in the Prometheus text format, under the route prefix like every route (default
`http://<node>:8080/cloud/metrics`). It needs the same `Authorization: Bearer <auth-key>` as the API. Any node
answers for the whole cluster; values of other nodes come from their heartbeats and are a few seconds old.

| Metric | Labels | Meaning |
|--------|--------|---------|
| `pocketcloud_node_up` | `node` | 1 if the node is reachable, 0 if not. |
| `pocketcloud_node_head` | `node` | 1 for the head. |
| `pocketcloud_node_cpu_percent` | `node` | CPU usage of the node's machine. |
| `pocketcloud_node_memory_used_megabytes` | `node` | Heap used by the cloud process. |
| `pocketcloud_node_memory_max_megabytes` | `node` | `maxMemoryMb` of the node for its servers (0 = no limit). |
| `pocketcloud_node_servers` | `node` | Servers running on the node. |
| `pocketcloud_servers` | `template`, `status` | Servers of the cluster. |
| `pocketcloud_players` | `template` | Online players by the template of their server. |
| `pocketcloud_players_total` | | Online players of the cluster. |

CPU and memory are missing for a node that is down. Example `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: pocketcloud
    metrics_path: /cloud/metrics
    authorization:
      credentials: <auth-key from http.yml>
    static_configs:
      - targets: ["node-1:8080", "node-2:8080"]
```

Scraping more than one node keeps the metrics available when a node fails; the values are the same, so use
`max by (node)` / `max by (template)` (or `max without (instance)`) in queries.

A ready Grafana dashboard is in [grafana-dashboard.json](grafana-dashboard.json): in Grafana under
**Dashboards → New → Import**, upload the file and pick your Prometheus data source.
