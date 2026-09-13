# PocketCloud HTTP API Documentation

> **TODO**: Move to wiki tab

## Table of Contents

- [Overview](#overview)
- [Base URL & Versioning](#base-url--versioning)
- [Authentication](#authentication)
- [Request Format](#request-format)
- [Response Format](#response-format)
- [Route Parameters & Query Strings](#route-parameters--query-strings)
- [Error Handling](#error-handling)
- [Endpoints](#endpoints)
  - [Health](#health)
  - [Stats](#stats)
  - [Servers](#servers)
  - [Players](#players)
  - [Templates](#templates)
  - [Groups](#groups)
  - [Plugins](#plugins)
  - [Maintenance](#maintenance)
  - [Notifications](#notifications)
- [Creating Custom Routes](#creating-custom-routes)
- [Server Configuration](#server-configuration)

---

## Overview

The PocketCloud HTTP server is a **Netty-based** HTTP/1.1 (and HTTP/1.0) API server built into PocketCloud.
It exposes information about servers, templates, players, groups, plugins, and cloud internals, and allows
external tools or dashboards to interact with the cloud over HTTP.

- Supports `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS`, and the custom `QUERY` method
- Built-in cloud routes are versioned under `/v1/` (see [Base URL & Versioning](#base-url--versioning))
- All request and response bodies use `application/json` unless a route explicitly returns another content type
  (e.g. `text/plain` for logs)
- Maximum request size: **10 MB** (enforced by Netty's `HttpObjectAggregator`; oversized requests are rejected
  automatically with `413 Payload Too Large`)
- Optional **TLS** support, either via a self-signed certificate generated at startup or a provided certificate/key
  file pair
- Keep-alive is honored: if the client requests `Connection: keep-alive`, the connection is kept open for subsequent
  requests; otherwise it is closed after the response is sent
- Cookies can be set by routes (with `Path`, `Domain`, `HttpOnly`, `Secure`, `Max-Age`, and `SameSite` support)
- Route handlers can enable CORS headers on a per-response basis

> **Note:** Earlier versions of this server supported request rate limiting and response caching. Neither feature
> exists in the current implementation.

---

## Base URL & Versioning

```
http://<host>:<port>/v1/<endpoint>
```
or, if a certificate is provided:
```
https://<host>:<port>/v1/<endpoint>
```

Built-in routes are versioned via an annotation on their controller class. The currently used version is **v1**.
The final path of a built-in route is assembled as:

```
<route prefix>/v<version><path>
```

- The **route prefix** is an optional prefix configured for the cloud's own routes (empty by default — see
  [Server Configuration](#server-configuration)). It is *not* applied to routes registered by plugins.
- Non-versioned routes (like `/health`) skip the `/v<version>` segment entirely, but still receive the route prefix
  if one is configured.

Versioned responses automatically receive an `X-API-Version` header stating which version served the request. If a
version has been marked deprecated, responses for that version also receive `Deprecation: true` and, if a sunset
date was configured, a `Sunset` header (per RFC 8594).

```http
HTTP/1.1 200 OK
X-API-Version: 1
Deprecation: true
Sunset: 2026-12-31
```

---

## Authentication

Routes that require authentication use `DefaultAuthentication`, which expects a **Bearer token** in the standard
`Authorization` header.

### Authenticated Request

```http
GET /v1/servers HTTP/1.1
Host: localhost:8080
Authorization: Bearer your-secret-token
```

| Header          | Required    | Description                                                                                                                               |
|-----------------|-------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `Authorization` | Conditional | Must be `Bearer <token>`, where `<token>` matches the cloud's configured auth token. Required on routes that use `DefaultAuthentication`. |

If authentication fails, the server responds with:

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "error": "Unauthorized",
  "message": "Unauthorized"
}
```

> **Note:** All built-in `/v1/` routes use `DefaultAuthentication` by default. The only exception is `GET /health`,
> which uses `NonRequiredAuthentication` and is always publicly accessible. When registering your own routes, you can
> supply any implementation of `IAuthentication`, and a matching `AuthenticationFailedHandler` to control what
> happens (and what response is sent) when authentication fails.

---

## Request Format

### Headers

| Header          | Required                      | Description                                                            |
|-----------------|-------------------------------|------------------------------------------------------------------------|
| `Content-Type`  | Recommended for body requests | Should be `application/json` for routes expecting a JSON body          |
| `Authorization` | Conditional                   | `Bearer <token>`, required when the route uses `DefaultAuthentication` |

### Body

Request bodies are parsed directly into the type declared by the route handler's `@RequestBody` parameter (via
Gson), or read as a raw JSON object/string when a route parses it manually. There is no per-route payload size limit
in the current implementation — only the global 10 MB request size cap applies. Malformed JSON results in:

```http
HTTP/1.1 400 Bad Request
```

```json
{
  "error": "Bad Request",
  "message": "Invalid JSON body: <parser detail>"
}
```

---

## Response Format

All responses are JSON objects unless otherwise noted (e.g. the log endpoint returns plain text). Responses always
include:

```http
Content-Type: application/json; charset=UTF-8
Content-Length: <bytes>
```

If the incoming request asked to keep the connection alive, the response also includes `Connection: keep-alive` and
the socket is kept open; otherwise the connection is closed right after the response is flushed.

Successful responses typically return `200 OK` with a JSON body — either the requested resource(s) or a simple
`{"message": "..."}` acknowledgement for actions. Error responses always have the shape:

```json
{
  "error": "<HTTP reason phrase>",
  "message": "<human-readable explanation>"
}
```

---

## Route Parameters & Query Strings

Routes can include dynamic path segments using `{param}` syntax. Inside a route handler method, such a segment is
bound to a method parameter annotated with `@PathVariable`:

**Example route path:** `/servers/{name}`

**Example request:** `GET /v1/servers/lobby-1`

```java
@GetRoute("/servers/{name}")
public void info(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
    // name == "lobby-1"
}
```

Multiple parameters per route are supported, e.g. `/servers/{name}/players/{uuid}`.

Query string values are read directly from the request object rather than bound automatically:

```java
String value = request.queryParam("key");          // single value, or null
String value = request.queryParam("key", "default"); // with a default
boolean present = request.hasQueryParam("key");
List<String> values = request.queryParams("key");    // all values for repeated keys
```

---

## Error Handling

| Status Code                 | Meaning                                                                                                                                         |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `400 Bad Request`           | Malformed HTTP request, invalid/missing JSON fields, or a thrown validation error                                                               |
| `401 Unauthorized`          | Authentication failed                                                                                                                           |
| `404 Not Found`             | No route matched the requested path (also returned for a matching path with an unsupported method — the method is not distinguished separately) |
| `409 Conflict`              | The requested operation conflicts with the current state (e.g. a template/group already exists, or server capacity has been reached)            |
| `413 Payload Too Large`     | Request body exceeds the 10 MB size limit                                                                                                       |
| `500 Internal Server Error` | An unhandled exception occurred while processing the request                                                                                    |

Every error response includes `error` (the HTTP reason phrase) and `message` (a human-readable explanation) fields.

---

## Endpoints

### Health

#### `GET /health`

Returns the health status of the HTTP server. This is a non-versioned, unauthenticated route.

**Request**

```http
GET /health HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
{
  "status": "ok"
}
```

---

### Stats

#### `GET /v1/stats`

Returns a snapshot of the current cloud instance statistics.

**Request**

```http
GET /v1/stats HTTP/1.1
Host: localhost:8080
Authorization: Bearer your-secret-token
```

**Response — `200 OK`**

```json
{
  "version": "1.0.0",
  "beta": false,
  "server_count": 4,
  "player_count": 12,
  "template_count": 3,
  "server_group_count": 2,
  "plugin_count": 5,
  "uptime_ms": 3600000,
  "total_avg_traffic": { "in": 1024, "out": 2048 },
  "total_traffic": { "in": 104857600, "out": 209715200 },
  "tps": 20.0,
  "avg_tps": 19.87,
  "tick_usage": 0.42,
  "used_memory": 512.0,
  "max_memory": 2048.0,
  "cpu_usage": 0.15
}
```

| Field                   | Type     | Description                                  |
|-------------------------|----------|----------------------------------------------|
| `version`               | `string` | The running PocketCloud version string       |
| `beta`                  | `bool`   | Whether this is a beta build                 |
| `server_count`          | `int`    | Number of currently running cloud servers    |
| `player_count`          | `int`    | Number of currently connected players        |
| `template_count`        | `int`    | Number of registered templates               |
| `server_group_count`    | `int`    | Number of registered server groups           |
| `plugin_count`          | `int`    | Number of loaded cloud plugins               |
| `uptime_ms`             | `long`   | Cloud instance uptime in **milliseconds**    |
| `total_avg_traffic.in`  | `long`   | Average inbound HTTP traffic in bytes        |
| `total_avg_traffic.out` | `long`   | Average outbound HTTP traffic in bytes       |
| `total_traffic.in`      | `long`   | Total inbound HTTP traffic in bytes          |
| `total_traffic.out`     | `long`   | Total outbound HTTP traffic in bytes         |
| `tps`                   | `double` | Current ticks-per-second of the cloud        |
| `avg_tps`               | `double` | Average ticks-per-second                     |
| `tick_usage`            | `double` | Fraction of the tick budget currently in use |
| `used_memory`           | `double` | Currently used process memory                |
| `max_memory`            | `double` | Maximum available process memory             |
| `cpu_usage`             | `double` | Current process CPU usage                    |

> **Note:** `uptime` changed from seconds to `uptime_ms` (milliseconds), and several performance fields (`tps`,
> `avg_tps`, `tick_usage`, `used_memory`, `max_memory`, `cpu_usage`) are new compared to earlier versions of this
> endpoint.

---

### Servers

#### `GET /v1/servers`

Returns **all** currently known cloud servers as an object keyed by server name (not an array).

**Request**

```http
GET /v1/servers HTTP/1.1
Host: localhost:8080
Authorization: Bearer your-secret-token
```

**Response — `200 OK`**

```json
{
  "Lobby-1": {
    "name": "Lobby-1",
    "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "status": "ONLINE",
    "playerCount": 5,
    "maxPlayers": 20
  }
}
```

| Field         | Type     | Description                                       |
|---------------|----------|---------------------------------------------------|
| `name`        | `string` | The server's display name                         |
| `uuid`        | `string` | Unique identifier for the server instance         |
| `status`      | `string` | Current server status (e.g. `ONLINE`, `STARTING`) |
| `playerCount` | `int`    | Number of players currently on the server         |
| `maxPlayers`  | `int`    | Maximum player capacity                           |

---

#### `QUERY /v1/servers`

Filters servers using a structured search query, sent as the request body. The body must match the shape expected
by `ServerSearchQuery`. Returns the same per-server summary shape as `GET /v1/servers`, keyed by server name.

**Request**

```http
QUERY /v1/servers HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Authorization: Bearer your-secret-token

{ "...": "see ServerSearchQuery for the accepted filter fields" }
```

**Error Responses**

| Code              | Condition                              | Body                                                                |
|-------------------|----------------------------------------|---------------------------------------------------------------------|
| `400 Bad Request` | A filter value has the wrong data type | `{"message": "Please use the correct data types for your values."}` |

---

#### `GET /v1/servers/{name}`

Returns detailed information about a single server, looked up by name or UUID.

**Route Parameters**

| Parameter | Type     | Description         |
|-----------|----------|---------------------|
| `name`    | `string` | Server name or UUID |

**Response — `200 OK`**

Returns the server's full internal state, plus:

| Field         | Type       | Description                               |
|---------------|------------|-------------------------------------------|
| `playerCount` | `int`      | Number of players currently on the server |
| `players`     | `string[]` | Names of players currently on the server  |

**Error Responses**

| Code              | Condition             | Body                                                   |
|-------------------|-----------------------|--------------------------------------------------------|
| `400 Bad Request` | `name` not provided   | `{"message": "Please specify a server name or uuid."}` |
| `404 Not Found`   | Server does not exist | `{"message": "Server not found."}`                     |

---

#### `GET /v1/servers/{name}/logs`

Retrieves the log output of a running server as plain text.

**Response — `200 OK`**

```
Content-Type: text/plain; charset=UTF-8
```

Returns the server's log lines as a newline-separated plain text body.

**Error Responses**

| Code                        | Condition                   | Body                                                   |
|-----------------------------|-----------------------------|--------------------------------------------------------|
| `400 Bad Request`           | `name` not provided         | `{"message": "Please specify a server name or uuid."}` |
| `404 Not Found`             | Server does not exist       | `{"message": "Server not found."}`                     |
| `500 Internal Server Error` | Logs could not be retrieved | `{"message": "Failed to retrieve server logs."}`       |

---

#### `POST /v1/servers/{name}/save`

Persists the server's current configuration to disk.

**Response — `200 OK`**

```json
{ "message": "Attempted to save the server." }
```

---

#### `POST /v1/servers/{name}/dispatch`

Dispatches a console command on the server.

**Request Body**

| Field     | Type     | Required | Description                     |
|-----------|----------|----------|---------------------------------|
| `command` | `string` | Yes      | The console command to dispatch |

**Response — `200 OK`**

```json
{ "message": "Attempted to dispatch the command on the server." }
```

---

#### `POST /v1/servers/start`

Starts one or more servers from a given template.

**Request Body**

| Field      | Type     | Required | Description                                       |
|------------|----------|----------|---------------------------------------------------|
| `template` | `string` | Yes      | Name of the template to start from                |
| `count`    | `int`    | Yes      | Number of server instances to start (must be ≥ 1) |

**Response — `200 OK`**

```json
{
  "message": "Attempted to start 2 server(s).",
  "started_servers": ["..."]
}
```

**Error Responses**

| Code                        | Condition                                               | Body                                                                                       |
|-----------------------------|---------------------------------------------------------|--------------------------------------------------------------------------------------------|
| `400 Bad Request`           | Template does not exist                                 | `{"message": "Template does not exist."}`                                                  |
| `400 Bad Request`           | `count` is less than 1                                  | `{"message": "The requested amount cannot be less than 1."}`                               |
| `409 Conflict`              | The template's server capacity has already been reached | `{"message": "The maximum amount of servers for this template has already been reached."}` |
| `500 Internal Server Error` | Starting the server(s) failed                           | `{"message": "Failed to start servers"}`                                                   |

---

#### `POST /v1/servers/stopAll`

Stops all currently running servers.

**Request Body**

| Field   | Type   | Required | Default | Description                               |
|---------|--------|----------|---------|-------------------------------------------|
| `force` | `bool` | No       | `false` | Force-stop instead of a graceful shutdown |

**Response — `200 OK`**

Returns the stopped servers.

**Error Responses**

| Code                        | Condition               | Body                                    |
|-----------------------------|-------------------------|-----------------------------------------|
| `500 Internal Server Error` | Stopping servers failed | `{"message": "Failed to stop servers"}` |

---

#### `POST /v1/servers/{name}/stop`

Stops a single server.

**Request Body**

| Field   | Type   | Required | Default | Description                               |
|---------|--------|----------|---------|-------------------------------------------|
| `force` | `bool` | No       | `false` | Force-stop instead of a graceful shutdown |

**Response — `200 OK`**

Returns the stopped server(s).

**Error Responses**

| Code                        | Condition                  | Body                                   |
|-----------------------------|----------------------------|----------------------------------------|
| `404 Not Found`             | Server does not exist      | `{"message": "Server not found."}`     |
| `500 Internal Server Error` | Stopping the server failed | `{"message": "Failed to stop server"}` |

---

### Players

#### `GET /v1/players`

Returns all currently connected players as an object keyed by player name.

**Response — `200 OK`**

```json
{
  "Steve": {
    "name": "Steve",
    "xboxUserId": "...",
    "server": "Lobby-1",
    "proxy": "Proxy-1"
  }
}
```

| Field        | Type     | Description                                   |
|--------------|----------|-----------------------------------------------|
| `name`       | `string` | The player's name                             |
| `xboxUserId` | `string` | The player's Xbox user ID                     |
| `server`     | `string` | Name of the server the player is currently on |
| `proxy`      | `string` | Name of the proxy the player is currently on  |

---

#### `QUERY /v1/players`

Filters players using a structured search query (see `PlayerSearchQuery`), sent as the request body. Returns the
same summary shape as `GET /v1/players`, keyed by player name.

**Error Responses**

| Code              | Condition                              | Body                                                                |
|-------------------|----------------------------------------|---------------------------------------------------------------------|
| `400 Bad Request` | A filter value has the wrong data type | `{"message": "Please use the correct data types for your values."}` |

---

#### `GET /v1/players/{name}`

Returns detailed information about a single player.

**Response — `200 OK`**

```json
{
  "name": "Steve",
  "uniqueId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "xboxUserId": "...",
  "protocolVersion": 686,
  "gameVersion": "1.21.0",
  "server": "Lobby-1",
  "proxy": "Proxy-1"
}
```

**Error Responses**

| Code              | Condition               | Body                                           |
|-------------------|-------------------------|------------------------------------------------|
| `400 Bad Request` | `name` not provided     | `{"message": "Please specify a player name."}` |
| `404 Not Found`   | Player is not connected | `{"message": "Player not found."}`             |

---

#### `POST /v1/players/{name}/kick`

Kicks a player from the network.

**Request Body**

| Field                     | Type     | Required | Default | Description                                     |
|---------------------------|----------|----------|---------|-------------------------------------------------|
| `reason`                  | `string` | No       | `""`    | The kick reason                                 |
| `disconnectScreenMessage` | `string` | No       | `""`    | Message shown on the client's disconnect screen |

**Response — `200 OK`**

```json
{ "message": "Kicked the player." }
```

---

#### `POST /v1/players/{name}/text`

Sends a message, popup, tip, title, action bar, or toast to a player.

**Request Body**

| Field     | Type     | Required | Default                        | Description                                                       |
|-----------|----------|----------|--------------------------------|-------------------------------------------------------------------|
| `type`    | `string` | Yes      | —                              | One of `MESSAGE`, `POPUP`, `TIP`, `TITLE`, `ACTION_BAR`, `TOAST`  |
| `title`   | `string` | No       | `""`                           | Used by `POPUP`, `TITLE`, `TOAST`                                 |
| `body`    | `string` | No       | `title` if set, otherwise `""` | Used by `MESSAGE`, `POPUP`, `TIP`, `TITLE`, `ACTION_BAR`, `TOAST` |
| `fadeIn`  | `int`    | No       | `1`                            | Used by `ACTION_BAR`                                              |
| `stay`    | `int`    | No       | `0`                            | Used by `ACTION_BAR`                                              |
| `fadeOut` | `int`    | No       | `1`                            | Used by `ACTION_BAR`                                              |

**Response — `200 OK`**

```json
{ "message": "Attempted to text the player." }
```

**Error Responses**

| Code              | Condition            | Body                                       |
|-------------------|----------------------|--------------------------------------------|
| `400 Bad Request` | Unknown `type` value | `{"message": "Invalid type name: <type>"}` |

---

#### `POST /v1/players/{name}/transfer`

Transfers a player to a different server.

**Request Body**

| Field    | Type     | Required | Description                    |
|----------|----------|----------|--------------------------------|
| `server` | `string` | Yes      | Name of the destination server |

**Response — `200 OK`**

```json
{ "message": "Attempted to transfer the player." }
```

**Error Responses**

| Code              | Condition                    | Body                               |
|-------------------|------------------------------|------------------------------------|
| `400 Bad Request` | Destination server not found | `{"message": "Server not found."}` |

---

### Templates

#### `GET /v1/templates`

Returns all templates as an object keyed by template name.

**Response — `200 OK`**

```json
{
  "Lobby": {
    "name": "Lobby",
    "playerCount": 5,
    "maxPlayers": 100,
    "serverCount": 2,
    "maxServers": 5,
    "lobby": true,
    "maintenance": false,
    "software": "BEDROCK"
  }
}
```

---

#### `QUERY /v1/templates`

Filters templates via a `TemplateSearchQuery` request body. Returns the same summary shape, keyed by name.

**Error Responses**

| Code              | Condition                              | Body                                                                |
|-------------------|----------------------------------------|---------------------------------------------------------------------|
| `400 Bad Request` | A filter value has the wrong data type | `{"message": "Please use the correct data types for your values."}` |

---

#### `GET /v1/templates/{name}`

Returns detailed information about a single template: its full internal state, plus `playerCount`, `players`
(names of players currently on it), and `parentGroups` (names of groups it belongs to).

**Error Responses**

| Code              | Condition               | Body                                             |
|-------------------|-------------------------|--------------------------------------------------|
| `400 Bad Request` | `name` not provided     | `{"message": "Please specify a template name."}` |
| `404 Not Found`   | Template does not exist | `{"message": "Template not found."}`             |

---

#### `POST /v1/templates`

Creates a new template. The body must include at least `name`, plus whatever additional fields the template
definition requires.

**Response — `200 OK`**

```json
{ "message": "Created the template." }
```

**Error Responses**

| Code           | Condition                                | Body                                      |
|----------------|------------------------------------------|-------------------------------------------|
| `409 Conflict` | A template with this name already exists | `{"message": "Template already exists."}` |

---

#### `PATCH /v1/templates/{name}`

Edits an existing template. The body shape follows the fields accepted by `TemplateEditData`.

**Response — `200 OK`**

```json
{ "message": "Edited the template." }
```

**Error Responses**

| Code              | Condition                             | Body                                                                |
|-------------------|---------------------------------------|---------------------------------------------------------------------|
| `404 Not Found`   | Template does not exist               | `{"message": "Template not found."}`                                |
| `400 Bad Request` | An edit value has the wrong data type | `{"message": "Please use the correct data types for your values."}` |

---

#### `DELETE /v1/templates/{name}`

Removes a template.

**Response — `200 OK`**

```json
{ "message": "Removed the template." }
```

---

### Groups

#### `GET /v1/groups`

Returns all server groups as an object keyed by group name.

**Response — `200 OK`**

```json
{
  "SkyBlock": {
    "name": "SkyBlock",
    "playerCount": 8
  }
}
```

---

#### `QUERY /v1/groups`

Filters groups via a `ServerGroupSearchQuery` request body. Returns the same summary shape, keyed by name.

---

#### `GET /v1/groups/{name}`

Returns detailed information about a single group: its full internal state, plus `players` and `player_count`.

**Error Responses**

| Code              | Condition            | Body                                          |
|-------------------|----------------------|-----------------------------------------------|
| `400 Bad Request` | `name` not provided  | `{"message": "Please specify a group name."}` |
| `404 Not Found`   | Group does not exist | `{"message": "Group not found."}`             |

---

#### `POST /v1/groups`

Creates a new server group. The body must include at least `name`, plus whatever additional fields the group
definition requires.

**Response — `200 OK`**

```json
{ "message": "Created the group." }
```

**Error Responses**

| Code           | Condition                             | Body                                   |
|----------------|---------------------------------------|----------------------------------------|
| `409 Conflict` | A group with this name already exists | `{"message": "Group already exists."}` |

---

#### `DELETE /v1/groups/{name}`

Removes a server group.

**Response — `200 OK`**

```json
{ "message": "Removed the group." }
```

---

#### `POST /v1/groups/{name}/templates`

Adds one or more templates to a group. Unknown template names are silently skipped.

**Request Body**

| Field       | Type       | Required | Description                                |
|-------------|------------|----------|--------------------------------------------|
| `templates` | `string[]` | Yes      | Names of the templates to add to the group |

**Response — `200 OK`**

```json
{ "message": "Added the templates to the group." }
```

---

#### `DELETE /v1/groups/{name}/templates`

Removes one or more templates from a group. Unknown template names are silently skipped.

**Request Body**

| Field       | Type       | Required | Description                                     |
|-------------|------------|----------|-------------------------------------------------|
| `templates` | `string[]` | Yes      | Names of the templates to remove from the group |

**Response — `200 OK`**

```json
{ "message": "Removed the templates from the group." }
```

---

### Plugins

#### `GET /v1/plugins`

Returns all loaded plugins as an object keyed by plugin name.

**Query Parameters**

| Parameter | Type   | Required | Default | Description                                      |
|-----------|--------|----------|---------|--------------------------------------------------|
| `enabled` | `bool` | No       | `false` | If `true`, only return currently enabled plugins |

**Response — `200 OK`**

```json
{
  "MyPlugin": {
    "name": "MyPlugin",
    "version": "1.0.0",
    "authors": ["Someone"]
  }
}
```

---

#### `GET /v1/plugins/{name}`

Returns detailed information about a single plugin.

**Response — `200 OK`**

```json
{
  "name": "MyPlugin",
  "status": "enabled",
  "version": "1.0.0",
  "main": "com.example.MyPlugin",
  "dataFolder": "/path/to/plugins/MyPlugin"
}
```

**Error Responses**

| Code              | Condition             | Body                                           |
|-------------------|-----------------------|------------------------------------------------|
| `400 Bad Request` | `name` not provided   | `{"message": "Please specify a plugin name."}` |
| `404 Not Found`   | Plugin does not exist | `{"message": "Plugin not found."}`             |

---

#### `POST /v1/plugins/{name}/enable`

Enables a plugin. If it is already enabled, returns `{"message": "Plugin is already enabled."}` without changing
anything.

**Response — `200 OK`**

```json
{ "message": "Plugin has been enabled." }
```

---

#### `POST /v1/plugins/{name}/disable`

Disables a plugin. If it is already disabled, returns `{"message": "Plugin is already disabled."}` without changing
anything.

**Response — `200 OK`**

```json
{ "message": "Plugin has been disabled." }
```

---

#### `POST /v1/plugins/enableAll`

Enables all plugins.

**Response — `200 OK`**

```json
{ "message": "All plugins have been enabled." }
```

---

#### `POST /v1/plugins/disableAll`

Disables all plugins.

**Response — `200 OK`**

```json
{ "message": "All plugins have been disabled." }
```

---

### Maintenance

The maintenance endpoints manage the whitelist used while the cloud (or a template) is in maintenance mode.

#### `GET /v1/maintenance`

Returns all currently whitelisted player names.

**Response — `200 OK`**

```json
["Steve", "Alex"]
```

---

#### `POST /v1/maintenance`

Adds a player to the whitelist.

**Request Body**

| Field    | Type     | Required | Description                         |
|----------|----------|----------|-------------------------------------|
| `player` | `string` | Yes      | The name of the player to whitelist |

**Response — `200 OK`**

```json
{ "message": "Player has been added to the whitelist." }
```

---

#### `DELETE /v1/maintenance`

Removes a player from the whitelist.

**Request Body**

| Field    | Type     | Required | Description                      |
|----------|----------|----------|----------------------------------|
| `player` | `string` | Yes      | The name of the player to remove |

**Response — `200 OK`**

```json
{ "message": "Player has been removed from the whitelist." }
```

---

#### `GET /v1/maintenance/{name}`

Checks whether a single player is whitelisted.

**Response — `200 OK`**

```json
{ "whitelisted": true }
```

---

### Notifications

The notifications endpoints manage which players have cloud notifications enabled.

#### `GET /v1/notifications`

Returns all players who currently have notifications enabled.

**Response — `200 OK`**

```json
["Steve", "Alex"]
```

---

#### `POST /v1/notifications`

Enables notifications for a player.

**Request Body**

| Field    | Type     | Required | Description                                        |
|----------|----------|----------|----------------------------------------------------|
| `player` | `string` | Yes      | The name of the player to enable notifications for |

**Response — `200 OK`**

```json
{ "message": "Player's notifications have been enabled." }
```

---

#### `DELETE /v1/notifications`

Disables notifications for a player.

**Request Body**

| Field    | Type     | Required | Description                                         |
|----------|----------|----------|-----------------------------------------------------|
| `player` | `string` | Yes      | The name of the player to disable notifications for |

**Response — `200 OK`**

```json
{ "message": "Player's notifications have been disabled." }
```

---

#### `GET /v1/notifications/{name}`

Checks whether a single player has notifications enabled.

**Response — `200 OK`**

```json
{ "enabled": true }
```

---

## Creating Custom Routes

Custom routes are plain Java classes whose methods are annotated with one of the route annotations. There is no
longer a base class to extend — the router discovers annotated methods via reflection when the controller instance
is registered.

### Annotated Controller (most common)

```java
import de.pocketcloud.cloud.http.Router;
import de.pocketcloud.cloud.http.annotation.ApiVersion;
import de.pocketcloud.cloud.http.annotation.GetRoute;
import de.pocketcloud.cloud.http.annotation.PathVariable;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;

@ApiVersion(1) // optional: default version + deprecation info for every route below
public final class GetServerRoute {

    @GetRoute("/servers/{name}")
    public void info(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        CloudServer server = PocketCloud.instance().servers().get(name).orElse(null);
        if (server == null) throw new HttpException(400, "Server not found.");

        response.json(obj -> obj.addProperty("name", server.getName()));
    }
}
```

Register the controller once, e.g. during plugin startup:

```java
public void registerControllers() {
    PocketCloud.instance().httpServer().router().registerController(new GetServerRoute());
}
```

> **Note:** Routes registered this way are **not** prefixed with the cloud's configured route prefix — only the
> cloud's own built-in controllers receive that prefix. This keeps plugin routes stable regardless of how the cloud
> is configured.

### Available Route Annotations

`@GetRoute`, `@PostRoute`, `@PutRoute`, `@PatchRoute`, `@DeleteRoute`, `@HeadRoute`, `@OptionsRoute`, and
`@QueryRoute` (for the custom `QUERY` HTTP method, typically used for search/filter-style endpoints that need a
request body). Each takes:

| Attribute        | Default                          | Description                                     |
|------------------|----------------------------------|-------------------------------------------------|
| `value`          | —                                | The route path, supports `{param}` placeholders |
| `authentication` | `DefaultAuthentication.class`    | An `IAuthentication` implementation             |
| `onAuthFailed`   | `DefaultAuthFailedHandler.class` | An `AuthenticationFailedHandler` implementation |
| `version`        | `-1` (unversioned)               | Overrides the class-level `@ApiVersion`, if any |

A class-level `@ApiVersion(value, deprecated, sunset)` sets the default version for every route method in that
class, and can mark that version as deprecated (adding `Deprecation`/`Sunset` response headers automatically).

### Method Parameters

Route handler methods can declare, in any order:

| Parameter type                 | Resolved from                                                                                   |
|--------------------------------|-------------------------------------------------------------------------------------------------|
| `@PathVariable("name") String` | The matching `{name}` path segment                                                              |
| `@RequestBody SomeType`        | The request body, deserialized via Gson (or the raw `String` if the parameter type is `String`) |
| `HttpRequest`                  | The current request object                                                                      |
| `HttpResponse`                 | The current response object                                                                     |

Throwing an `HttpException(statusCode, message)` from within a handler aborts the request and sends a JSON error
response with that status code and message.

### Functional Registration (no annotations)

Routes can also be registered directly on the router without a controller class:

```java
public void examples() {
    Router router = PocketCloud.instance().httpServer().router();
    router.get("/status", (req, res) -> res.json(obj -> obj.addProperty("alive", true)));
    router.get("/status", (req, res) -> res.json(obj -> obj.addProperty("alive", true)), NonRequiredAuthentication.class);
}
```

Overloads exist for `get`, `post`, `put`, `patch`, `delete`, `head`, `options`, and `query`, each optionally taking
an `IAuthentication` class, an `AuthenticationFailedHandler` class, and/or an explicit API version.

### Reading Request Data

| Method                                        | Description                                           |
|-----------------------------------------------|-------------------------------------------------------|
| `request.pathParam("name")`                   | Route parameter from path, e.g. `{name}`              |
| `request.queryParam("key")`                   | Single query string value, or `null`                  |
| `request.queryParam("key", "def")`            | Query string value with a default                     |
| `request.queryParams("key")`                  | All values for a repeated query key                   |
| `request.hasQueryParam("key")`                | Check if a query string key exists                    |
| `request.header("key")`                       | HTTP request header value                             |
| `request.body()`                              | Raw request body string                               |
| `request.json()` / `request.json(Type.class)` | Body parsed as a `JsonObject` or a given type         |
| `request.bearerToken()`                       | The `Authorization: Bearer <token>` value, if present |

### Writing Responses

| Method                                                                             | Description                                                                   |
|------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| `response.status(code)`                                                            | Sets the HTTP status code                                                     |
| `response.json(...)` / `.text(...)` / `.html(...)`                                 | Sends a body with the corresponding content type                              |
| `response.header(name, value)`                                                     | Sets a response header                                                        |
| `response.cookie(name, value, builder -> ...)`                                     | Adds a `Set-Cookie` header (path, domain, secure, httpOnly, maxAge, sameSite) |
| `response.cors(origin)`                                                            | Adds permissive CORS headers for the given origin                             |
| `response.attachment(filename)`                                                    | Marks the response as a downloadable attachment                               |
| `response.file(file, contentType)`                                                 | Streams a file as the response body                                           |
| `response.redirect(location)` / `.redirectPermanent(location)`                     | Sends a `302`/`301` redirect                                                  |
| `response.badRequest/unauthorized/forbidden/notFound/internalServerError(message)` | Shorthand error responses                                                     |

---

## Server Configuration

The HTTP server is built from the cloud's `MainConfig`. Fields confirmed to be in use by the current implementation:

| Config Area                            | Description                                                                     |
|----------------------------------------|---------------------------------------------------------------------------------|
| `http_server.route_prefix`             | Optional prefix applied in front of every built-in cloud route (e.g. `/v1/...`) |
| `http_server.ssl.enabled`              | Whether the HTTP server should use TLS                                          |
| `http_server.ssl.self_signed`          | If `true`, generates a self-signed certificate at startup                       |
| `http_server.ssl.self_signed_hostname` | Hostname used for the self-signed certificate's subject/SAN                     |
| `http_server.ssl.cert_file`            | Path to a certificate file (used when not self-signed)                          |
| `http_server.ssl.private_key_file`     | Path to the certificate's private key file                                      |
| `http_server.ssl.key_password`         | Optional password for the private key file                                      |

The server's auth token (used by `DefaultAuthentication`) is generated via a secure random token generator and
exposed as `HttpServer#authToken()`.

> **Note:** The exact `MainConfig`/`storage/config.yml` key names above are inferred from how they are consumed in
> the HTTP server code; the config class itself wasn't part of the reviewed source. Rate limiting and response
> caching configuration from earlier versions of this document no longer apply — those features have been removed.