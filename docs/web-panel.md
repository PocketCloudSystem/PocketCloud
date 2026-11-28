# Web panel

PocketCloud ships a small web interface in `pocketcloud.jar`. It runs on the HTTP server of the cloud, so there is
nothing extra to install.

## Opening it

1. In `storage/configs/http.yml` set `enabled: true` and pick `address` and `port`.
2. Restart the cloud (or `reload`).
3. Open `http://<address>:<port>/cloud/panel/` (`/cloud` is the `routePrefix` of `http.yml`).
4. Create a user in the cloud console with `user create <name> admin` and sign in with the printed password.

In a cluster every node with HTTP enabled serves the panel. Each node shows the whole cluster; actions are forwarded
to the head or the node that runs a server, like the [HTTP API](cluster/http-api.md#forwarding-rules).

## Pages

| Page | What you can do |
|------|-----------------|
| Overview | Players, servers, templates, uptime, charts of the last 24 h (players, servers, CPU, memory), CPU/memory/TPS of this cloud process, nodes of the cluster |
| Console | The cloud console of the node you are connected to: live log, run any cloud command (confirmations are answered with `y`/`n`). Commands from the panel are logged as `[Panel] > ...` |
| Servers | Filter, start servers of a template, stop all; per server: live data, save, stop, force stop, live console with commands, server files (read only) |
| Templates | Create, edit all settings (incl. `nodes`/`staticNode`), start a server, rolling restart, delete; browse, edit, upload and delete the template files (text files up to 1 MB open in an editor, Ctrl+S saves); file history with rollback (cluster) |
| Groups | Create, delete, add and remove templates |
| Players | Filter, send a message/popup/title/..., transfer, kick |
| Maintenance | Players that may join templates in maintenance: add, remove |
| Nodes | State, load, version, drain/undrain (cluster) |
| Schedules | Scheduled console commands: add with a cron time, remove |
| Backups | Create, restore (the zips stay on the node: they contain secrets) |
| Plugins | Cloud plugins of this node: enable, disable |
| Crashes | Crash reports of servers, per node |
| Users | Panel users and their permissions (only with `admin`) |

The charts are sampled once a minute by the node you are connected to and kept in memory, so they start empty
after a restart. CPU and memory are those of that node's cloud process; players and servers count the whole cluster.

The buttons at the top switch between light and dark (default: the system setting) and between German and
English (default: the browser language). Both choices are kept in the browser. Messages from the server stay English.

## Users and roles

The panel only accepts panel users (the auth-key still works for the REST API as bearer token). Manage them in the
cloud console:

| Command | |
|---------|--|
| `user create <name> <role>` | Creates the user and prints a random password once (it also lands in the log file) |
| `user role <name> <role>` | Changes the role; the user is signed out |
| `user password <name>` | Prints a new random password; the user is signed out |
| `user remove <name>` | Deletes the user |
| `user list` | Users and roles |

Admins can do the same on the *Users* page. Everyone changes their own password with the key next to their name.
Passwords have at least 8 characters and are stored as PBKDF2 hashes in `storage/internal/panel/users.json`.

Every user may look at everything; the role decides what they may change:

| Role | May change |
|------|------------|
| `admin` | Everything, including users, nodes, backups, schedules, plugins and the cloud console |
| `developer` | Servers, templates and their files, groups, maintenance |
| `moderator` | Servers, players |
| `supporter` | Players (message, transfer, kick) |
| `viewer` | Nothing |

Missing rights answer `403`. Users are stored on each node separately: copy `users.json` to a node if people should
sign in there too.

## Security

- The password is only sent once at login. The browser then holds a session cookie (`HttpOnly`, `SameSite=Strict`,
  12 hours, `Secure` when TLS is on). Sessions live in memory: a restart signs everyone out.
- Requests that change something also need the header `X-PocketCloud-Panel`, which other websites cannot set
  (CSRF protection).
- 5 wrong logins per minute and address are allowed, then login answers `429`.
- Without TLS the password and the cookie travel in plain text. Outside a trusted network enable TLS in `http.yml` or put
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
