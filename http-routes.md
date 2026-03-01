# PocketCloud HTTP Route Ideas

## 🖥️ Servers — `/v1/servers`

| Method | Path                         | Description                                                     |
|--------|------------------------------|-----------------------------------------------------------------|
| `GET`  | `/v1/servers`                | List all running servers (supports filtering by template/group) |
| `GET`  | `/v1/servers/{name}`         | Get a specific server by name                                   |
| `POST` | `/v1/servers/start`          | Start server(s) from a template (`template`, `count` in body)   |
| `POST` | `/v1/servers/{name}/stop`    | Stop a specific server (`force` flag in body)                   |
| `POST` | `/v1/servers/stopAll`        | Stop all servers (`force` flag in body)                         |
| `POST` | `/v1/servers/{name}/save`    | Save a specific server's state                                  |
| `POST` | `/v1/servers/{name}/command` | Send a command to a running server remotely                     |
| `GET`  | `/v1/servers/{name}/logs`    | Get recent output/logs from a specific server                   |

---

## 📋 Templates — `/v1/templates`

| Method   | Path                   | Description                                                       |
|----------|------------------------|-------------------------------------------------------------------|
| `GET`    | `/v1/templates`        | List all templates (filterable by `TemplateType`)                 |
| `GET`    | `/v1/templates/{name}` | Get a specific template                                           |
| `POST`   | `/v1/templates`        | Create a new template                                             |
| `PATCH`  | `/v1/templates/{name}` | Edit template settings (maintenance, maxPlayers, autoStart, etc.) |
| `DELETE` | `/v1/templates/{name}` | Remove a template                                                 |

---

## 👥 Players — `/v1/players`

| Method | Path                        | Description                                                   |
|--------|-----------------------------|---------------------------------------------------------------|
| `GET`  | `/v1/players`               | List all online players (filterable by server/template/group) |
| `GET`  | `/v1/players/{name}`        | Get a specific player                                         |
| `POST` | `/v1/players/{name}/kick`   | Kick a player                                                 |
| `POST` | `/v1/players/{name}/switch` | Switch a player to another server                             |
| `POST` | `/v1/players/{name}/text`   | Send a text to a specific player                              |

---

## 🗂️ Server Groups — `/v1/groups`

| Method   | Path                                 | Description                    |
|----------|--------------------------------------|--------------------------------|
| `GET`    | `/v1/groups`                         | List all server groups         |
| `GET`    | `/v1/groups/{name}`                  | Get a specific group           |
| `POST`   | `/v1/groups`                         | Create a new server group      |
| `DELETE` | `/v1/groups/{name}`                  | Remove a server group          |
| `POST`   | `/v1/groups/{name}/templates`        | Add a template to a group      |
| `DELETE` | `/v1/groups/{name}/templates/{name}` | Remove a template from a group |

---

## 🔌 Plugins — `/v1/plugins`

| Method | Path                         | Description                                       |
|--------|------------------------------|---------------------------------------------------|
| `GET`  | `/v1/plugins`                | List all plugins (with optional `enabled` filter) |
| `GET`  | `/v1/plugins/{name}`         | Get a specific plugin                             |
| `POST` | `/v1/plugins/{name}/enable`  | Enable a plugin                                   |
| `POST` | `/v1/plugins/{name}/disable` | Disable a plugin                                  |

---

## 🔔 Notifications — `/v1/notifications`

| Method   | Path                         | Description                                        |
|----------|------------------------------|----------------------------------------------------|
| `GET`    | `/v1/notifications`          | List all current players with enabled notifcations |
| `POST`   | `/v1/notifications`          | Enable notifications for a player                  |
| `DELETE` | `/v1/notifications/{player}` | Disable notifications for a player                 |

---

## 🔒 Maintenance — `/v1/maintenance`

| Method   | Path                       | Description                                  |
|----------|----------------------------|----------------------------------------------|
| `GET`    | `/v1/maintenance`          | List all current players who are whitelisted |
| `POST`   | `/v1/maintenance`          | Whitelist a player                           |
| `DELETE` | `/v1/maintenance/{player}` | Remove a player from the whitelist           |

---

## 📊 Meta & Health

| Method | Path        | Description                                                     |
|--------|-------------|-----------------------------------------------------------------|
| `GET`  | `/health`   | Simple health check — always returns `200 OK`, no auth required |
| `GET`  | `/v1/stats` | Cloud-wide stats: server count, player count, uptime, traffic   |

---

## Implementation Notes

- All `/v1/...` routes should extend `ApiPath`, which automatically prefixes the version via `getFullPath()`.
- `/health` should be a `RegularPath` with `NoAuthRequiredAuthentication` and a minimal response body, e.g. `{ "status": "ok" }`.
- Write operations (`POST`, `PATCH`, `DELETE`) should use `DefaultAuthentication`. Read-only routes can use `NoAuthRequiredAuthentication` depending on your security requirements.
- The `RateLimiter` utility is already in place — consider applying it to write routes to prevent abuse.
- The `HttpTrafficMonitor` is already hooked in, so the `/v1/stats` route can expose that data directly.
