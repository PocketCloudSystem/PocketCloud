# Web panel

PocketCloud ships a small web interface in `pocketcloud.jar`. It runs on the HTTP server of the cloud, so there is
nothing extra to install.

## Opening it

1. In `storage/configs/http.yml` set `enabled: true` and pick `address` and `port`.
2. Restart the cloud (or `reload`).
3. Open `http://<address>:<port>/cloud/panel/` (`/cloud` is the `routePrefix` of `http.yml`).
4. Sign in with the `auth-key` from `http.yml`.

In a cluster every node with HTTP enabled serves the panel. Each node shows the whole cluster; actions are forwarded
to the head or the node that runs a server, like the [HTTP API](cluster/http-api.md#forwarding-rules).

## Pages

| Page | What you can do |
|------|-----------------|
| Overview | Players, servers, templates, uptime, CPU/memory/TPS of this cloud process, nodes of the cluster |
| Servers | Filter, start servers of a template, stop all; per server: live data, save, stop, force stop, live console with commands |
| Templates | Create, edit all settings (incl. `nodes`/`staticNode`), start a server, rolling restart, delete; file history with rollback (cluster) |
| Players | Filter, send a message/popup/title/..., transfer, kick |
| Nodes | State, load, version, drain/undrain (cluster) |
| Schedules | Scheduled console commands: add with a cron time, remove |
| Backups | Create, restore (the zips stay on the node: they contain secrets) |

## Security

- The `auth-key` is only sent once at login. The browser then holds a session cookie (`HttpOnly`, `SameSite=Strict`,
  12 hours, `Secure` when TLS is on). Sessions live in memory: a restart signs everyone out.
- Requests that change something also need the header `X-PocketCloud-Panel`, which other websites cannot set
  (CSRF protection).
- 5 wrong keys per minute and address are allowed, then login answers `429`.
- Without TLS the key and the cookie travel in plain text. Outside a trusted network enable TLS in `http.yml` or put
  a reverse proxy with HTTPS in front.

## Building

The panel is built by Maven (`frontend-maven-plugin`): it downloads Node.js into `target/`, runs `npm ci` and
`npm run build` in `cloud-core/src/main/panel`, and puts the result into the jar under `panel/`.

```bash
mvn package                    # with panel
mvn package -DskipPanel        # without; /cloud/panel/ then answers 404
```

The first build needs internet access (Node.js and npm packages). With `-DskipPanel` after an earlier build, run
`mvn clean` too, otherwise the old panel files stay in `target/classes`.

Developing the panel with hot reload:

```bash
cd cloud-core/src/main/panel
npm install
PANEL_TARGET=http://127.0.0.1:8080 npm run dev   # proxies /cloud/... to a running cloud
```

The panel uses Preact and Vite, no other runtime dependencies.
