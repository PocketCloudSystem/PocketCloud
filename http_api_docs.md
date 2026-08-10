# PocketCloud HTTP API Documentation

> **TODO**: Move to wiki tab

## Table of Contents

- [Overview](#overview)
- [Base URL & Versioning](#base-url--versioning)
- [Authentication](#authentication)
- [Rate Limiting](#rate-limiting)
- [Response Caching](#response-caching)
- [Request Format](#request-format)
- [Response Format](#response-format)
- [Route Parameters](#route-parameters)
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

The PocketCloud HTTP server is a lightweight, socket-based HTTP/1.1 API server built into the PocketCloud system. It
exposes information about servers, templates, players, and cloud internals, and allows external tools or dashboards to
interact with the cloud over HTTP.

- Supports `GET`, `POST`, `PUT`, `DELETE`, and `PATCH` methods
- All API endpoints are versioned under `/v1/`
- All request and response bodies use `application/json`
- Maximum request size: **10 MB**
- Maximum headers per request: **100**

---

## Base URL & Versioning

```
http://<host>:<port>/v1/<endpoint>
```

The server supports API versioning. The currently available version is **v1**. All versioned routes must be registered
against a known `ApiVersion` and will be rejected if versioning is disabled on the server.

Non-versioned routes (like `/health`) are accessible directly at the root:

```
http://<host>:<port>/health
```

---

## Authentication

Most API routes use `DefaultAuthentication` and are only accessible through an `auth-key` header.

### Authenticated Request

```http
GET /v1/servers HTTP/1.1
Host: localhost:8080
auth-key: your-secret-key
```

| Header     | Required    | Description                                                                                     |
|------------|-------------|-------------------------------------------------------------------------------------------------|
| `auth-key` | Conditional | The secret key configured in `MainConfig`. Required on routes that use `DefaultAuthentication`. |

If authentication fails, the server responds with:

```http
HTTP/1.1 403 Forbidden
```

> **Note:** Individual route files specify which authentication strategy they use. By default, all built-in v1 routes
> use `NoAuthRequiredAuthentication` but are still locked behind the `DefaultAuthentication` due to the specified
`ApiVersion`, which is `v1`. You can pass a custom `Authentication` implementation when registering your own routes.

---

## Rate Limiting

The server supports optional rate limiting, configured via `MainConfig` aka `storage/config.yml`. When enabled, each
client IP is tracked.

| Parameter               | Default | Description                                                       |
|-------------------------|---------|-------------------------------------------------------------------|
| `max_requests`          | 10      | Maximum number of requests allowed in the time frame              |
| `time_frame_in_seconds` | 10      | Rolling window in seconds for counting requests                   |
| `timeout_in_seconds`    | 120     | How long (in seconds) the IP is blocked after exceeding the limit |

When a client exceeds the limit, the server responds with:

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
```

```json
{
  "message": "You are being rate limited. Please try again in 118 seconds.",
  "end_timestamp": 1712345678
}
```

| Field           | Type     | Description                                        |
|-----------------|----------|----------------------------------------------------|
| `message`       | `string` | Human-readable message including seconds remaining |
| `end_timestamp` | `int`    | Unix timestamp when the rate limit expires         |

---

## Response Caching

The server supports optional response caching for `200 OK` responses. Caching is keyed on the combination of API
version, HTTP method, full path, and sorted query parameters. The cache is automatically invalidated after the
configured `caching_time_in_seconds`.

When caching is enabled, repeated identical requests are served from memory without re-executing route logic.

---

## Request Format

### Headers

| Header           | Required                   | Description                                                     |
|------------------|----------------------------|-----------------------------------------------------------------|
| `Content-Type`   | Required for body requests | Must be `application/json` for any request that includes a body |
| `Content-Length` | Recommended                | Length of the request body in bytes                             |
| `auth-key`       | Conditional                | Required when the route uses `DefaultAuthentication`            |

### Body

Request bodies must be valid JSON and must not exceed the route's configured `maxPayloadLength`. Sending a body larger
than the limit results in:

```http
HTTP/1.1 413 Payload Too Large
```

---

## Response Format

All responses are JSON objects unless otherwise noted. Responses always include the following HTTP headers:

```http
Content-Type: application/json
Content-Length: <bytes>
Connection: close
```

Successful responses return `200 OK`. Error responses include a `message` field explaining what went wrong and if an
exception occurred during the process, the exception itself is returned.

---

## Route Parameters

Routes can include dynamic path segments using `{param}` syntax. The parameter value can be accessed inside the route
handler via `$request->getParameter("param")`.

**Example route path:** `/servers/{name}`

**Example request:** `GET /v1/servers/lobby-1`

Inside the route handler:

```php
$name = $request->getParameter("name"); // "lobby-1"
```

Multiple parameters per route are supported, e.g. `/servers/{name}/players/{uuid}`.

---

## Error Handling

| Status Code                           | Meaning                                                                                           |
|---------------------------------------|---------------------------------------------------------------------------------------------------|
| `400 Bad Request`                     | Missing or invalid request body / headers                                                         |
| `403 Forbidden`                       | Authentication failed                                                                             |
| `404 Not Found`                       | No route matched the requested path and method                                                    |
| `405 Method Not Allowed`              | HTTP method is not in the supported list                                                          |
| `409 Conflict`                        | The requested operation conflicts with the current state (e.g. server capacity reached)           |
| `413 Payload Too Large`               | Request or body exceeds the size limit                                                            |
| `429 Too Many Requests`               | Client has been rate limited                                                                      |
| `431 Request Header Fields Too Large` | More than 100 headers were sent                                                                   |
| `500 Internal Server Error`           | Route's `willCauseError()` check returned true, or an exception was thrown during request parsing |
| `505 HTTP Version Not Supported`      | Protocol was not HTTP/1.0 or HTTP/1.1                                                             |

---

## Endpoints

### Health

#### `GET /health`

Returns the health status of the HTTP server. This is a non-versioned route and requires no authentication.

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
  "uptime": 3600,
  "total_avg_traffic": {
    "in": 1024,
    "out": 2048
  },
  "total_traffic": {
    "in": 104857600,
    "out": 209715200
  }
}
```

| Field                   | Type     | Description                               |
|-------------------------|----------|-------------------------------------------|
| `version`               | `string` | The running PocketCloud version string    |
| `beta`                  | `bool`   | Whether this is a beta build              |
| `server_count`          | `int`    | Number of currently running cloud servers |
| `player_count`          | `int`    | Number of currently connected players     |
| `template_count`        | `int`    | Number of registered templates            |
| `server_group_count`    | `int`    | Number of registered server groups        |
| `plugin_count`          | `int`    | Number of loaded cloud plugins            |
| `uptime`                | `int`    | Server uptime in seconds                  |
| `total_avg_traffic.in`  | `int`    | Average inbound traffic in bytes          |
| `total_avg_traffic.out` | `int`    | Average outbound traffic in bytes         |
| `total_traffic.in`      | `int`    | Total inbound traffic in bytes            |
| `total_traffic.out`     | `int`    | Total outbound traffic in bytes           |

---

### Servers

#### `GET /v1/servers`

Returns a list of all currently running cloud servers. Optionally filter by template or server group.

**Query Parameters**

| Parameter  | Type     | Required | Description                                              |
|------------|----------|----------|----------------------------------------------------------|
| `template` | `string` | No       | Filter results to servers belonging to this template     |
| `group`    | `string` | No       | Filter results to servers belonging to this server group |

**Request**

```http
GET /v1/servers?template=Lobby HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
[
  {
    "name": "Lobby-1",
    "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "player_count": 5,
    "max_players": 20,
    "status": "ONLINE"
  }
]
```

| Field          | Type               | Description                                       |
|----------------|--------------------|---------------------------------------------------|
| `name`         | `string`           | The server's display name                         |
| `uuid`         | `string`           | Unique identifier for the server instance         |
| `player_count` | `int`              | Number of players currently on the server         |
| `max_players`  | `int`              | Maximum player capacity                           |
| `status`       | `string` \| `null` | Current server status (e.g. `ONLINE`, `STARTING`) |

**Error Responses**

| Code              | Condition                                           | Body                                         |
|-------------------|-----------------------------------------------------|----------------------------------------------|
| `400 Bad Request` | `template` refers to a template that doesn't exist  | `{"message": "Template does not exist."}`    |
| `400 Bad Request` | `group` refers to a server group that doesn't exist | `{"message": "ServerGroup does not exist."}` |

---

#### `GET /v1/servers/{name}`

Returns detailed information about a single server, looked up by name or UUID.

**Route Parameters**

| Parameter | Type     | Description         |
|-----------|----------|---------------------|
| `name`    | `string` | Server name or UUID |

**Request**

```http
GET /v1/servers/Lobby-1 HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

Returns the full server object as produced by `$server->write()`.

**Error Responses**

| Code              | Condition                     | Body                                                   |
|-------------------|-------------------------------|--------------------------------------------------------|
| `400 Bad Request` | `name` parameter not provided | `{"message": "Please specify a server name or uuid."}` |
| `400 Bad Request` | Server does not exist         | `{"message": "Server not found."}`                     |

---

#### `GET /v1/servers/{name}/logs`

Retrieves the log output of a running server as plain text.

**Route Parameters**

| Parameter | Type     | Description         |
|-----------|----------|---------------------|
| `name`    | `string` | Server name or UUID |

**Request**

```http
GET /v1/servers/Lobby-1/logs HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```
Content-Type: text/plain; charset=utf-8
```

Returns the server's log lines as a newline-separated plain text body.

**Error Responses**

| Code                        | Condition                     | Body                                                   |
|-----------------------------|-------------------------------|--------------------------------------------------------|
| `400 Bad Request`           | `name` parameter not provided | `{"message": "Please specify a server name or uuid."}` |
| `400 Bad Request`           | Server does not exist         | `{"message": "Server not found."}`                     |
| `500 Internal Server Error` | Logs could not be retrieved   | `{"message": "Failed to retrieve server logs."}`       |

---

#### `POST /v1/servers/start`

Starts one or more servers from a given template.

**Request**

```http
POST /v1/servers/start HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "template": "Lobby",
  "count": 2
}
```

**Request Body**

| Field      | Type     | Required | Description                         |
|------------|----------|----------|-------------------------------------|
| `template` | `string` | Yes      | Name of the template to start from  |
| `count`    | `int`    | Yes      | Number of server instances to start |

**Response — `200 OK`**

```json
{
  "message": "Attempted to start 2 server(s).",
  "started_servers": ["Lobby-1", "Lobby-2"]
}
```

| Field             | Type       | Description                            |
|-------------------|------------|----------------------------------------|
| `message`         | `string`   | Confirmation message                   |
| `started_servers` | `string[]` | Names of the servers that were started |

**Error Responses**

| Code              | Condition                            | Body                                                                                       |
|-------------------|--------------------------------------|--------------------------------------------------------------------------------------------|
| `400 Bad Request` | Template does not exist              | `{"message": "Template does not exist."}`                                                  |
| `400 Bad Request` | `count` is less than 1               | `{"message": "The requested amount cannot be less than 1."}`                               |
| `409 Conflict`    | Maximum server count already reached | `{"message": "The maximum amount of servers for this template has already been reached."}` |

---

#### `POST /v1/servers/{name}/stop`

Stops one or more servers matching a name, UUID, template name, or group name.

**Route Parameters**

| Parameter | Type     | Description                                     |
|-----------|----------|-------------------------------------------------|
| `name`    | `string` | Server name, UUID, template name, or group name |

**Request**

```http
POST /v1/servers/Lobby-1/stop HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "force": false
}
```

**Request Body**

| Field   | Type   | Required | Description                                        |
|---------|--------|----------|----------------------------------------------------|
| `force` | `bool` | Yes      | Whether to forcefully terminate the server process |

**Response — `200 OK`**

```json
[
  {"name": "Lobby-1", "uuid": "a1b2c3d4-..."}
]
```

Returns an array of objects for each stopped server, each with `name` and `uuid`.

**Error Responses**

| Code              | Condition                     | Body                                                                                           |
|-------------------|-------------------------------|------------------------------------------------------------------------------------------------|
| `400 Bad Request` | `name` parameter not provided | `{"message": "Please specify a server name or uuid, a template name or a server group name."}` |
| `400 Bad Request` | No matching servers found     | `{"message": "Server(s) not found."}`                                                          |

---

#### `POST /v1/servers/stopAll`

Stops all currently running servers.

**Request**

```http
POST /v1/servers/stopAll HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "force": true
}
```

**Request Body**

| Field   | Type   | Required | Description                                          |
|---------|--------|----------|------------------------------------------------------|
| `force` | `bool` | Yes      | Whether to forcefully terminate all server processes |

**Response — `200 OK`**

```json
[
  {"name": "Lobby-1", "uuid": "a1b2c3d4-..."},
  {"name": "Lobby-2", "uuid": "b2c3d4e5-..."}
]
```

Returns an array of all stopped servers, each with `name` and `uuid`.

---

#### `POST /v1/servers/{name}/save`

Triggers a save operation on the specified server.

**Route Parameters**

| Parameter | Type     | Description         |
|-----------|----------|---------------------|
| `name`    | `string` | Server name or UUID |

**Request**

```http
POST /v1/servers/Lobby-1/save HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
{
  "message": "Attempted to save the server."
}
```

**Error Responses**

| Code              | Condition                     | Body                                                   |
|-------------------|-------------------------------|--------------------------------------------------------|
| `400 Bad Request` | `name` parameter not provided | `{"message": "Please specify a server name or uuid."}` |
| `400 Bad Request` | Server does not exist         | `{"message": "Server not found."}`                     |

---

#### `POST /v1/servers/{name}/execute`

Executes a command on the specified server.

**Route Parameters**

| Parameter | Type     | Description         |
|-----------|----------|---------------------|
| `name`    | `string` | Server name or UUID |

**Request**

```http
POST /v1/servers/Lobby-1/execute HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "command": "op Steve"
}
```

**Request Body**

| Field     | Type     | Required | Max Length | Description                      |
|-----------|----------|----------|------------|----------------------------------|
| `command` | `string` | Yes      | 256 bytes  | The command to run on the server |

**Response — `200 OK`**

```json
{
  "message": "Attempted to execute the command on the server."
}
```

**Error Responses**

| Code              | Condition                     | Body                                                   |
|-------------------|-------------------------------|--------------------------------------------------------|
| `400 Bad Request` | `name` parameter not provided | `{"message": "Please specify a server name or uuid."}` |
| `400 Bad Request` | Server does not exist         | `{"message": "Server not found."}`                     |

---

### Players

#### `GET /v1/players`

Returns a list of all currently connected players. Optionally filter by server, template, or group. Only one filter may
be applied at a time.

**Query Parameters**

| Parameter  | Type     | Required | Description                                        |
|------------|----------|----------|----------------------------------------------------|
| `server`   | `string` | No       | Filter players by the server they are currently on |
| `template` | `string` | No       | Filter players by template                         |
| `group`    | `string` | No       | Filter players by server group                     |

**Request**

```http
GET /v1/players?server=Lobby-1 HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
{
  "Steve": {
    "name": "Steve",
    "xbox_id": "2535416402234234",
    "server": "Lobby-1",
    "proxy": "Proxy-1"
  }
}
```

| Field     | Type               | Description                         |
|-----------|--------------------|-------------------------------------|
| `name`    | `string`           | The player's name                   |
| `xbox_id` | `string`           | The player's Xbox User ID           |
| `server`  | `string` \| `null` | Name of the server the player is on |
| `proxy`   | `string` \| `null` | Name of the proxy the player is on  |

**Error Responses**

| Code              | Condition                                           | Body                                                                                       |
|-------------------|-----------------------------------------------------|--------------------------------------------------------------------------------------------|
| `400 Bad Request` | `server` refers to a server that doesn't exist      | `{"message": "Server does not exist."}`                                                    |
| `400 Bad Request` | `template` refers to a template that doesn't exist  | `{"message": "Template does not exist."}`                                                  |
| `400 Bad Request` | `group` refers to a server group that doesn't exist | `{"message": "ServerGroup does not exist."}`                                               |
| `400 Bad Request` | More than one filter was provided simultaneously    | `{"message": "You can only apply one of the following filters: server, template, group."}` |

---

#### `GET /v1/players/{name}`

Returns detailed information about a single player.

**Route Parameters**

| Parameter | Type     | Description       |
|-----------|----------|-------------------|
| `name`    | `string` | The player's name |

**Request**

```http
GET /v1/players/Steve HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

Returns the full player object as produced by `$player->write()`.

**Error Responses**

| Code              | Condition             | Body                                           |
|-------------------|-----------------------|------------------------------------------------|
| `400 Bad Request` | `name` not provided   | `{"message": "Please specify a player name."}` |
| `400 Bad Request` | Player does not exist | `{"message": "Player not found."}`             |

---

#### `POST /v1/players/{name}/kick`

Kicks a player from the network.

**Route Parameters**

| Parameter | Type     | Description       |
|-----------|----------|-------------------|
| `name`    | `string` | The player's name |

**Request**

```http
POST /v1/players/Steve/kick HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "reason": "banned",
  "disconnectScreenMessage": "You are banned."
}
```

**Request Body**

| Field                     | Type     | Required | Max Length | Description                                         |
|---------------------------|----------|----------|------------|-----------------------------------------------------|
| `reason`                  | `string` | No       | 512 bytes  | The internal reason for the kick                    |
| `disconnectScreenMessage` | `string` | No       | 512 bytes  | The message shown on the player's disconnect screen |

**Response — `200 OK`**

```json
{
  "message": "Kicked the player."
}
```

**Error Responses**

| Code              | Condition             | Body                                           |
|-------------------|-----------------------|------------------------------------------------|
| `400 Bad Request` | `name` not provided   | `{"message": "Please specify a player name."}` |
| `400 Bad Request` | Player does not exist | `{"message": "Player not found."}`             |

---

#### `POST /v1/players/{name}/text`

Sends a message, title, or other text packet to a player.

**Route Parameters**

| Parameter | Type     | Description       |
|-----------|----------|-------------------|
| `name`    | `string` | The player's name |

**Request**

```http
POST /v1/players/Steve/text HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "type": "MESSAGE",
  "message": "Hello!"
}
```

**Request Body**

| Field     | Type     | Required | Max Length | Description                                                                           |
|-----------|----------|----------|------------|---------------------------------------------------------------------------------------|
| `type`    | `string` | Yes      | 1024 bytes | Text type — one of the available `TextType` values (e.g. `MESSAGE`, `TITLE`, `POPUP`) |
| `message` | `string` | Yes      | 1024 bytes | The text content to send to the player                                                |

**Response — `200 OK`**

```json
{
  "message": "Attempted to text the player."
}
```

**Error Responses**

| Code              | Condition                      | Body                                           |
|-------------------|--------------------------------|------------------------------------------------|
| `400 Bad Request` | `name` not provided            | `{"message": "Please specify a player name."}` |
| `400 Bad Request` | Player does not exist          | `{"message": "Player not found."}`             |
| `400 Bad Request` | `type` is not a valid TextType | `{"message": "TextType not found."}`           |

---

#### `POST /v1/players/{name}/transfer`

Transfers a player to a different server.

**Route Parameters**

| Parameter | Type     | Description       |
|-----------|----------|-------------------|
| `name`    | `string` | The player's name |

**Request**

```http
POST /v1/players/Steve/transfer HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "server": "BedWars-1"
}
```

**Request Body**

| Field    | Type     | Required | Max Length | Description                       |
|----------|----------|----------|------------|-----------------------------------|
| `server` | `string` | Yes      | 64 bytes   | Name of the server to transfer to |

**Response — `200 OK`**

```json
{
  "message": "Attempted to transfer the player."
}
```

**Error Responses**

| Code              | Condition             | Body                                           |
|-------------------|-----------------------|------------------------------------------------|
| `400 Bad Request` | `name` not provided   | `{"message": "Please specify a player name."}` |
| `400 Bad Request` | Player does not exist | `{"message": "Player not found."}`             |
| `400 Bad Request` | Server does not exist | `{"message": "Server not found."}`             |

---

### Templates

#### `GET /v1/templates`

Returns a list of all registered templates. Optionally filter by type.

**Query Parameters**

| Parameter | Type     | Required | Description                                      |
|-----------|----------|----------|--------------------------------------------------|
| `type`    | `string` | No       | Filter by template type (e.g. `SERVER`, `PROXY`) |

**Request**

```http
GET /v1/templates HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
[
  {
    "name": "Lobby",
    "player_count": 10,
    "maintenance": false
  }
]
```

| Field          | Type     | Description                                       |
|----------------|----------|---------------------------------------------------|
| `name`         | `string` | Template name                                     |
| `player_count` | `int`    | Total players across all servers of this template |
| `maintenance`  | `bool`   | Whether the template is in maintenance mode       |

**Error Responses**

| Code              | Condition                              | Body                                                         |
|-------------------|----------------------------------------|--------------------------------------------------------------|
| `400 Bad Request` | `type` query refers to an unknown type | `{"message": "The specified template type does not exist."}` |

---

#### `GET /v1/templates/{name}`

Returns detailed information about a specific template.

**Route Parameters**

| Parameter | Type     | Description   |
|-----------|----------|---------------|
| `name`    | `string` | Template name |

**Request**

```http
GET /v1/templates/Lobby HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

Returns the full template object as produced by `$template->write()`.

**Error Responses**

| Code              | Condition               | Body                                 |
|-------------------|-------------------------|--------------------------------------|
| `400 Bad Request` | Template does not exist | `{"message": "Template not found."}` |

---

#### `POST /v1/templates/`

Creates a new template.

**Request**

```http
POST /v1/templates/ HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "name": "Lobby",
  "lobby": true,
  "maintenance": false,
  "static": false,
  "alwaysCopyToStaticServers": false,
  "maxPlayerCount": 20,
  "minServerCount": 1,
  "maxServerCount": 2,
  "startNewPercentage": 30.0,
  "autoStart": true,
  "templateType": "SERVER"
}
```

**Request Body**

| Field                       | Type             | Required | Description                                                    |
|-----------------------------|------------------|----------|----------------------------------------------------------------|
| `name`                      | `string`         | Yes      | Unique name for the template                                   |
| `lobby`                     | `bool`           | Yes      | Whether this is a lobby template                               |
| `maintenance`               | `bool`           | Yes      | Whether the template starts in maintenance mode                |
| `static`                    | `bool`           | Yes      | Whether servers from this template are static (persistent)     |
| `alwaysCopyToStaticServers` | `bool`           | Yes      | Whether to always copy files to static servers on start        |
| `maxPlayerCount`            | `int`            | Yes      | Maximum number of players per server                           |
| `minServerCount`            | `int`            | Yes      | Minimum number of servers to keep running                      |
| `maxServerCount`            | `int`            | Yes      | Maximum number of servers allowed to run simultaneously        |
| `startNewPercentage`        | `float` \| `int` | Yes      | Percentage fill at which a new server is automatically started |
| `autoStart`                 | `bool`           | Yes      | Whether servers auto-start to maintain `minServerCount`        |
| `templateType`              | `string`         | Yes      | Template type — `SERVER` or `PROXY`                            |

**Response — `200 OK`**

```json
{
  "message": "Created the template."
}
```

**Error Responses**

| Code              | Condition                    | Body                                      |
|-------------------|------------------------------|-------------------------------------------|
| `400 Bad Request` | Invalid template data        | `{"message": "Invalid template object."}` |
| `400 Bad Request` | Template name already exists | `{"message": "Template already exists."}` |

---

#### `PATCH /v1/templates/{name}`

Edits one or more fields of an existing template. All fields are optional — only the fields you include will be updated.

**Route Parameters**

| Parameter | Type     | Description   |
|-----------|----------|---------------|
| `name`    | `string` | Template name |

**Request**

```http
PATCH /v1/templates/Lobby HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "maxPlayerCount": 30,
  "maintenance": true
}
```

**Request Body**

All fields are optional. Any combination of the following editable keys may be sent:

| Field                       | Type             | Description                    |
|-----------------------------|------------------|--------------------------------|
| `lobby`                     | `bool`           | Lobby flag                     |
| `maintenance`               | `bool`           | Maintenance mode flag          |
| `static`                    | `bool`           | Static server flag             |
| `alwaysCopyToStaticServers` | `bool`           | Copy-to-static flag            |
| `maxPlayerCount`            | `int`            | Maximum players per server     |
| `minServerCount`            | `int`            | Minimum running servers        |
| `maxServerCount`            | `int`            | Maximum running servers        |
| `startNewPercentage`        | `float` \| `int` | Start-new threshold percentage |
| `autoStart`                 | `bool`           | Auto-start flag                |

**Response — `200 OK`**

```json
{
  "message": "Edited the template."
}
```

**Error Responses**

| Code              | Condition                        | Body                                                                        |
|-------------------|----------------------------------|-----------------------------------------------------------------------------|
| `400 Bad Request` | `name` not provided              | `{"message": "Please specify a template name."}`                            |
| `400 Bad Request` | Template does not exist          | `{"message": "Template not found."}`                                        |
| `400 Bad Request` | Body contains a non-editable key | `{"message": "The key: <key> is not allowed inside the request body."}`     |
| `400 Bad Request` | A field value has the wrong type | `{"message": "Invalid value for key: <key>, expected: <type>, got <type>"}` |

---

#### `DELETE /v1/templates/{name}`

Removes a template permanently.

**Route Parameters**

| Parameter | Type     | Description   |
|-----------|----------|---------------|
| `name`    | `string` | Template name |

**Request**

```http
DELETE /v1/templates/Lobby HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
{
  "message": "Removed the template."
}
```

**Error Responses**

| Code              | Condition               | Body                                             |
|-------------------|-------------------------|--------------------------------------------------|
| `400 Bad Request` | `name` not provided     | `{"message": "Please specify a template name."}` |
| `400 Bad Request` | Template does not exist | `{"message": "Template not found."}`             |

---

### Groups

#### `GET /v1/groups`

Returns a list of all registered server groups.

**Request**

```http
GET /v1/groups HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
[
  {
    "name": "BedWars",
    "player_count": 42
  }
]
```

| Field          | Type     | Description                                      |
|----------------|----------|--------------------------------------------------|
| `name`         | `string` | Group name                                       |
| `player_count` | `int`    | Total number of players across all group servers |

---

#### `GET /v1/groups/{name}`

Returns detailed information about a specific server group.

**Route Parameters**

| Parameter | Type     | Description |
|-----------|----------|-------------|
| `name`    | `string` | Group name  |

**Request**

```http
GET /v1/groups/BedWars HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

Returns the full group object as produced by `$group->write()`.

**Error Responses**

| Code              | Condition            | Body                                          |
|-------------------|----------------------|-----------------------------------------------|
| `400 Bad Request` | `name` not provided  | `{"message": "Please specify a group name."}` |
| `400 Bad Request` | Group does not exist | `{"message": "Group not found."}`             |

---

#### `POST /v1/groups/`

Creates a new server group.

**Request**

```http
POST /v1/groups/ HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "name": "BedWars",
  "templates": ["BW-2x1", "BW-2x4"]
}
```

**Request Body**

| Field       | Type       | Required | Description                                    |
|-------------|------------|----------|------------------------------------------------|
| `name`      | `string`   | Yes      | Unique name for the group                      |
| `templates` | `string[]` | Yes      | List of template names to include in the group |

**Response — `200 OK`**

```json
{
  "message": "Created the group."
}
```

**Error Responses**

| Code              | Condition                 | Body                                   |
|-------------------|---------------------------|----------------------------------------|
| `400 Bad Request` | Invalid group data        | `{"message": "Invalid group object."}` |
| `400 Bad Request` | Group name already exists | `{"message": "Group already exists."}` |

---

#### `DELETE /v1/groups/{name}`

Removes a server group permanently.

**Route Parameters**

| Parameter | Type     | Description |
|-----------|----------|-------------|
| `name`    | `string` | Group name  |

**Request**

```http
DELETE /v1/groups/BedWars HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
{
  "message": "Removed the group."
}
```

**Error Responses**

| Code              | Condition            | Body                                          |
|-------------------|----------------------|-----------------------------------------------|
| `400 Bad Request` | `name` not provided  | `{"message": "Please specify a group name."}` |
| `400 Bad Request` | Group does not exist | `{"message": "Group not found."}`             |

---

#### `POST /v1/groups/{name}/templates`

Adds one or more templates to an existing group. Templates that do not exist are silently skipped.

**Route Parameters**

| Parameter | Type     | Description |
|-----------|----------|-------------|
| `name`    | `string` | Group name  |

**Request**

```http
POST /v1/groups/BedWars/templates HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "templates": ["BW-2x1", "BW-2x4"]
}
```

**Request Body**

| Field       | Type       | Required | Max Length | Description                   |
|-------------|------------|----------|------------|-------------------------------|
| `templates` | `string[]` | Yes      | 512 bytes  | List of template names to add |

**Response — `200 OK`**

```json
{
  "message": "Added the templates to the group."
}
```

**Error Responses**

| Code              | Condition            | Body                                          |
|-------------------|----------------------|-----------------------------------------------|
| `400 Bad Request` | `name` not provided  | `{"message": "Please specify a group name."}` |
| `400 Bad Request` | Group does not exist | `{"message": "Group not found."}`             |

---

#### `DELETE /v1/groups/{name}/templates`

Removes one or more templates from an existing group. Templates not in the group are silently skipped.

**Route Parameters**

| Parameter | Type     | Description |
|-----------|----------|-------------|
| `name`    | `string` | Group name  |

**Request**

```http
DELETE /v1/groups/BedWars/templates HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "templates": ["BW-2x1"]
}
```

**Request Body**

| Field       | Type       | Required | Max Length | Description                      |
|-------------|------------|----------|------------|----------------------------------|
| `templates` | `string[]` | Yes      | 512 bytes  | List of template names to remove |

**Response — `200 OK`**

```json
{
  "message": "Removed the templates from the group."
}
```

**Error Responses**

| Code              | Condition            | Body                                          |
|-------------------|----------------------|-----------------------------------------------|
| `400 Bad Request` | `name` not provided  | `{"message": "Please specify a group name."}` |
| `400 Bad Request` | Group does not exist | `{"message": "Group not found."}`             |

---

### Plugins

#### `GET /v1/plugins`

Returns a list of all loaded cloud plugins. Pass an `enabled` filter in the body to narrow results.

**Request**

```http
GET /v1/plugins HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "enabled": true
}
```

**Request Body**

| Field     | Type   | Required | Description                                                         |
|-----------|--------|----------|---------------------------------------------------------------------|
| `enabled` | `bool` | Yes      | `true` to list only enabled plugins, `false` for only disabled ones |

**Response — `200 OK`**

```json
[
  {
    "name": "MyPlugin v1.0.0",
    "authors": ["Alice"],
    "version": "1.0.0"
  }
]
```

| Field     | Type       | Description                        |
|-----------|------------|------------------------------------|
| `name`    | `string`   | Full plugin name including version |
| `authors` | `string[]` | List of plugin authors             |
| `version` | `string`   | Plugin version string              |

---

#### `GET /v1/plugins/{name}`

Returns detailed information about a specific plugin.

**Route Parameters**

| Parameter | Type     | Description |
|-----------|----------|-------------|
| `name`    | `string` | Plugin name |

**Request**

```http
GET /v1/plugins/MyPlugin HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
{
  "name": "MyPlugin",
  "version": "1.0.0",
  "full_name": "MyPlugin v1.0.0",
  "authors": ["Alice"],
  "main": "alice\\myplugin\\MyPlugin",
  "src_namespace_prefix": "alice\\myplugin",
  "data_folder": "plugins/MyPlugin/"
}
```

| Field                  | Type       | Description                      |
|------------------------|------------|----------------------------------|
| `name`                 | `string`   | Plugin name                      |
| `version`              | `string`   | Plugin version string            |
| `full_name`            | `string`   | Full name including version      |
| `authors`              | `string[]` | List of plugin authors           |
| `main`                 | `string`   | Fully qualified main class name  |
| `src_namespace_prefix` | `string`   | Source namespace prefix          |
| `data_folder`          | `string`   | Path to the plugin's data folder |

**Error Responses**

| Code              | Condition             | Body                                           |
|-------------------|-----------------------|------------------------------------------------|
| `400 Bad Request` | `name` not provided   | `{"message": "Please specify a plugin name."}` |
| `400 Bad Request` | Plugin does not exist | `{"message": "Plugin not found."}`             |

---

#### `POST /v1/plugins/{name}/enable`

Enables a specific plugin. If the plugin is already enabled, a notice is returned without error.

**Route Parameters**

| Parameter | Type     | Description |
|-----------|----------|-------------|
| `name`    | `string` | Plugin name |

**Request**

```http
POST /v1/plugins/MyPlugin/enable HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
{"message": "Plugin has been enabled."}
```

If already enabled:

```json
{"message": "Plugin is already enabled."}
```

**Error Responses**

| Code              | Condition             | Body                                           |
|-------------------|-----------------------|------------------------------------------------|
| `400 Bad Request` | `name` not provided   | `{"message": "Please specify a plugin name."}` |
| `400 Bad Request` | Plugin does not exist | `{"message": "Plugin not found."}`             |

---

#### `POST /v1/plugins/{name}/disable`

Disables a specific plugin. If the plugin is already disabled, a notice is returned without error.

**Route Parameters**

| Parameter | Type     | Description |
|-----------|----------|-------------|
| `name`    | `string` | Plugin name |

**Request**

```http
POST /v1/plugins/MyPlugin/disable HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
{"message": "Plugin has been disabled."}
```

If already disabled:

```json
{"message": "Plugin is already disabled."}
```

**Error Responses**

| Code              | Condition             | Body                                           |
|-------------------|-----------------------|------------------------------------------------|
| `400 Bad Request` | `name` not provided   | `{"message": "Please specify a plugin name."}` |
| `400 Bad Request` | Plugin does not exist | `{"message": "Plugin not found."}`             |

---

#### `POST /v1/plugins/enableAll`

Enables all currently loaded plugins at once.

**Request**

```http
POST /v1/plugins/enableAll HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
{
  "message": "All plugins have been enabled."
}
```

---

#### `POST /v1/plugins/disableAll`

Disables all currently loaded plugins at once.

**Request**

```http
POST /v1/plugins/disableAll HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
{
  "message": "All plugins have been disabled."
}
```

---

### Maintenance

The maintenance endpoints manage the cloud's maintenance whitelist — the list of players who are allowed to join while
maintenance mode is active.

#### `GET /v1/maintenance`

Returns all players currently on the maintenance whitelist.

**Request**

```http
GET /v1/maintenance HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
["Steve", "Alex", "Notch"]
```

---

#### `POST /v1/maintenance`

Adds a player to the maintenance whitelist.

**Request**

```http
POST /v1/maintenance HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"player": "Steve"}
```

**Request Body**

| Field    | Type     | Required | Max Length | Description                         |
|----------|----------|----------|------------|-------------------------------------|
| `player` | `string` | Yes      | 32 bytes   | The name of the player to whitelist |

**Response — `200 OK`**

```json
{
  "message": "Player has been added to the whitelist."
}
```

---

#### `DELETE /v1/maintenance`

Removes a player from the maintenance whitelist.

**Request**

```http
DELETE /v1/maintenance HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"player": "Steve"}
```

**Request Body**

| Field    | Type     | Required | Max Length | Description                      |
|----------|----------|----------|------------|----------------------------------|
| `player` | `string` | Yes      | 32 bytes   | The name of the player to remove |

**Response — `200 OK`**

```json
{
  "message": "Player has been removed from the whitelist."
}
```

---

### Notifications

The notifications endpoints manage which players have cloud notifications enabled.

#### `GET /v1/notifications`

Returns all players who currently have notifications enabled.

**Request**

```http
GET /v1/notifications HTTP/1.1
Host: localhost:8080
```

**Response — `200 OK`**

```json
["Steve", "Alex"]
```

---

#### `POST /v1/notifications`

Enables notifications for a player.

**Request**

```http
POST /v1/notifications HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"player": "Steve"}
```

**Request Body**

| Field    | Type     | Required | Max Length | Description                                        |
|----------|----------|----------|------------|----------------------------------------------------|
| `player` | `string` | Yes      | 32 bytes   | The name of the player to enable notifications for |

**Response — `200 OK`**

```json
{
  "message": "Player's notifications have been enabled."
}
```

---

#### `DELETE /v1/notifications`

Disables notifications for a player.

**Request**

```http
DELETE /v1/notifications HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"player": "Steve"}
```

**Request Body**

| Field    | Type     | Required | Max Length | Description                                         |
|----------|----------|----------|------------|-----------------------------------------------------|
| `player` | `string` | Yes      | 32 bytes   | The name of the player to disable notifications for |

**Response — `200 OK`**

```json
{
  "message": "Player's notifications have been disabled."
}
```

---

## Creating Custom Routes

Custom routes are created by extending one of the base route classes.

### Versioned JSON Route (most common)

Extend `ApiV1JsonPath` for any route under `/v1/` that sends and receives JSON.

```php
use pocketcloud\cloud\http\server\io\Request;use pocketcloud\cloud\http\server\io\ResponseBuilder;use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;use pocketcloud\cloud\http\server\util\HttpConstants;

final class GetServerRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/servers/{name}",       // path (supports {param} placeholders)
            HttpConstants::GET,      // HTTP method
            0,                       // max payload length (0 = no body expected)
            [],                      // required body fields (e.g. ["player" => "string"])
            null                     // authentication (null = NoAuthRequired)
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $name = $request->getParameter("name");
        $server = CloudServerManager::getInstance()->get($name);
        $builder->body(["name" => $server->getName()]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $name = $request->getParameter("name");
        if (CloudServerManager::getInstance()->get($name) === null) {
            $response->body(["message" => "Server not found."]);
            return true; // abort with 400
        }
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
```

Then register it:

```php
HttpServer::getInstance()->registerPath(new GetServerRoute());
```

### Route with a Required Body

Set `maxPayloadLength` and provide a `requiredBodyStructure` to automatically validate the incoming JSON shape:

```php
parent::__construct(
    "/servers",
    HttpConstants::POST,
    256,                          // max body size in bytes
    ["name" => "string"]          // required fields and their expected types
);
```

If the incoming body does not match this structure, the server automatically returns `400 Bad Request` before
`checkForBadRequest()` or `onHandle()` are called.

### Non-Versioned Route

Extend `RegularPath` for routes that live outside the `/v1/` namespace:

```php
use pocketcloud\cloud\http\server\route\RegularPath;use pocketcloud\cloud\http\server\socket\auth\NoAuthRequiredAuthentication;use pocketcloud\cloud\http\server\util\HttpConstants;

final class StatusRoute extends RegularPath {

    public function __construct() {
        parent::__construct("/status", HttpConstants::GET, new NoAuthRequiredAuthentication());
    }

    public function handle(Request $request): Response {
        return ResponseBuilder::create()->code(StatusCode::OK)->body(["alive" => true])->build();
    }

    public function isBadRequest(Request $request, ResponseBuilder $response): bool { return false; }
    public function willCauseError(Request $request, ResponseBuilder $response): bool { return false; }
}
```

### Reading Request Data

| Method                           | Description                                  |
|----------------------------------|----------------------------------------------|
| `$request->getParameter("name")` | Route parameter from path, e.g. `{name}`     |
| `$request->hasParameter("name")` | Check if route parameter exists              |
| `$request->getParameters()`      | All route parameters as an associative array |
| `$request->getQuery("key")`      | URL query string value, e.g. `?key=value`    |
| `$request->hasQuery("key")`      | Check if query string key exists             |
| `$request->getHeader("key")`     | HTTP request header value                    |
| `$request->hasHeader("key")`     | Check if header exists                       |
| `$request->getBody()`            | Raw request body string                      |

---

## Server Configuration

The HTTP server is built using `HttpServerBuilder` and configured from `MainConfig`.

```php
HttpServerBuilder::buildFromConfig();
```

Relevant `MainConfig` fields:

| Config Key                                     | Description                                        |
|------------------------------------------------|----------------------------------------------------|
| `http_server.enabled`                          | Whether the HTTP server starts at all              |
| `http_server.address`                          | Bind address and port (e.g. `0.0.0.0:8080`)        |
| `http_server.only_local`                       | If `true`, only accepts connections from localhost |
| `http_server.auth_key`                         | The secret key used by `DefaultAuthentication`     |
| `http_server.rate_limit.enabled`               | Enable or disable rate limiting                    |
| `http_server.rate_limit.max_requests`          | Max requests per time frame before limiting        |
| `http_server.rate_limit.time_frame_in_seconds` | Time window for counting requests                  |
| `http_server.rate_limit.timeout_in_seconds`    | How long to block a rate-limited IP                |
| `http_server.caching.enabled`                  | Enable response caching                            |
| `http_server.caching.caching_time_in_seconds`  | How long cached responses are kept                 |