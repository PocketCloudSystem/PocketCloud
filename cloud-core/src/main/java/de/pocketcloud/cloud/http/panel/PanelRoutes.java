package de.pocketcloud.cloud.http.panel;

import de.pocketcloud.cloud.console.log.cache.ConsoleTail;
import de.pocketcloud.cloud.console.log.CloudLogger;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import de.pocketcloud.cloud.http.annotation.DeleteRoute;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pocketcloud.api.component.software.IServerSoftware;
import java.util.Comparator;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.http.annotation.GetRoute;
import de.pocketcloud.cloud.http.annotation.PathVariable;
import de.pocketcloud.cloud.http.annotation.PostRoute;
import de.pocketcloud.cloud.http.auth.NonRequiredAuthentication;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.CookieBuilder;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.util.ServerLogStream;
import de.pocketcloud.cloud.util.VersionInfo;
import de.pocketcloud.common.util.StringUtils;

/** Login and live data of the web panel ({@code <routePrefix>/panel-api/...}); everything else uses the normal API. */
public final class PanelRoutes {

    private static final long CONSOLE_TAIL_BYTES = 64 * 1024;

    /** Body {@code {"key": ...}} for the auth-key, or {@code {"user": ..., "password": ...}}. */
    @PostRoute(value = "/panel-api/login", authentication = NonRequiredAuthentication.class)
    public void login(HttpRequest request, HttpResponse response) {
        String address = request.remoteAddress();
        if (!PanelSessions.mayTry(address)) throw new HttpException(429, "Too many failed logins, try again in a minute.");
        String key = null, name = null, password = null;
        try {
            JsonObject body = request.json();
            if (body != null && body.has("key")) key = body.get("key").getAsString().trim();
            if (body != null && body.has("user")) name = body.get("user").getAsString().trim();
            if (body != null && body.has("password")) password = body.get("password").getAsString();
        } catch (RuntimeException ignored) {
            // no JSON body
        }
        String user;
        if (name != null && !name.isEmpty()) {
            user = password == null ? null : PanelUsers.check(name, password).map(PanelUsers.User::name).orElse(null);
        } else {
            user = key != null && StringUtils.secureEquals(key, PocketCloud.instance().httpServer().authToken()) ? PanelUsers.KEY_USER : null;
        }
        if (user == null) {
            PanelSessions.failed(address);
            throw new HttpException(401, name != null && !name.isEmpty() ? "Wrong name or password." : "Wrong auth-key.");
        }
        PanelSessions.succeeded(address);
        String prefix = PocketCloud.instance().httpServer().router().routePrefix();
        response.cookie(PanelSessions.COOKIE, PanelSessions.create(user), c -> c.path(prefix.isEmpty() ? "/" : prefix)
                .httpOnly(true).sameSite(CookieBuilder.SameSite.STRICT).maxAge(PanelSessions.LIFETIME.toSeconds())
                .secure(PocketCloud.instance().config().httpServer().ssl().enabled()));
        response.json(json -> info(json, PanelUsers.get(user).orElseThrow()));
    }

    @PostRoute(value = "/panel-api/logout", authentication = NonRequiredAuthentication.class)
    public void logout(HttpRequest request, HttpResponse response) {
        PanelSessions.remove(request);
        String prefix = PocketCloud.instance().httpServer().router().routePrefix();
        response.cookie(PanelSessions.COOKIE, "", c -> c.path(prefix.isEmpty() ? "/" : prefix).maxAge(0));
        response.noContent();
    }

    /** Who am I talking to; 401 tells the panel to show the login. */
    @GetRoute("/panel-api/session")
    public void session(HttpRequest request, HttpResponse response) {
        // a bearer token (API client) has no panel user: it has every right, like the auth-key login
        PanelUsers.User user = PanelSessions.user(request).orElseGet(() -> PanelUsers.get(PanelUsers.KEY_USER).orElseThrow());
        response.json(json -> info(json, user));
    }

    // ------------------------------------------------------------------ users (admin only, see PanelSessions)

    @GetRoute("/panel-api/users")
    public void users(HttpRequest request, HttpResponse response) {
        JsonArray list = new JsonArray();
        for (PanelUsers.User user : PanelUsers.list()) list.add(userJson(user));
        JsonObject json = new JsonObject();
        json.add("users", list);
        json.add("permissions", new Gson().toJsonTree(PanelUsers.PERMISSIONS));
        response.json(json);
    }

    /** Creates or updates: {@code {"name", "password" (empty keeps it), "permissions": [...]}}. */
    @PostRoute("/panel-api/users")
    public void saveUser(HttpRequest request, HttpResponse response) {
        JsonObject body = request.json();
        if (body == null || !body.has("name")) throw new HttpException(400, "Missing name.");
        List<String> permissions = new ArrayList<>();
        if (body.has("permissions")) body.getAsJsonArray("permissions").forEach(p -> permissions.add(p.getAsString()));
        String password = body.has("password") ? body.get("password").getAsString() : null;
        PanelUsers.User user = PanelUsers.save(body.get("name").getAsString().trim(), password, permissions);
        PanelSessions.removeUser(user.name()); // changed rights or password: log in again
        CloudLogger.get().info("Panel user §b{}§r was saved by §b{}§r.", user.name(), PanelSessions.user(request).map(PanelUsers.User::name).orElse("?"));
        response.json(userJson(user));
    }

    @DeleteRoute("/panel-api/users/{name}")
    public void deleteUser(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        if (!PanelUsers.remove(name)) throw new HttpException(404, "No such user.");
        PanelSessions.removeUser(name);
        CloudLogger.get().info("Panel user §b{}§r was deleted.", name);
        response.noContent();
    }

    private static JsonObject userJson(PanelUsers.User user) {
        JsonObject json = new JsonObject();
        json.addProperty("name", user.name());
        json.add("permissions", new Gson().toJsonTree(user.permissions()));
        return json;
    }

    /** Software names for the "new template" form. */
    @GetRoute("/panel-api/softwares")
    public void softwares(HttpRequest request, HttpResponse response) {
        JsonArray array = new JsonArray();
        PocketCloud.instance().softwares().getAll().stream().sorted(Comparator.comparing(IServerSoftware::name)).forEach(software -> {
            JsonObject json = new JsonObject();
            json.addProperty("name", software.name());
            json.addProperty("templateType", software.templateType());
            array.add(json);
        });
        response.json(array);
    }

    private void info(JsonObject json, PanelUsers.User user) {
        var cloud = PocketCloud.instance();
        json.addProperty("user", user.name());
        json.add("permissions", new Gson().toJsonTree(user.permissions().contains("admin") ? PanelUsers.PERMISSIONS : user.permissions()));
        json.addProperty("cloudName", cloud.config().cloudName());
        json.addProperty("node", cloud.cluster().nodeName());
        json.addProperty("clustered", cloud.cluster().clustered());
        json.addProperty("version", VersionInfo.VERSION + (VersionInfo.BETA ? "@BETA" : ""));
    }

    /**
     * Live console as Server-Sent Events: event {@code lines} with new log lines (one per data line), {@code end} when
     * the server is gone, {@code failure} if the log cannot be read. Works for servers of other nodes too.
     */
    @GetRoute("/panel-api/servers/{name}/console")
    public void console(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        if (!(PocketCloud.instance().servers().get(name).orElse(null) instanceof CloudServer server)) {
            throw new HttpException(404, "Server not found.");
        }
        HttpResponse.EventStream stream = response.eventStream();
        Thread.ofVirtual().name("panel-console-" + name).start(() -> {
            ServerLogStream logs = server.openLogStream();
            try {
                logs.startStream();
                logs.skipToTail(CONSOLE_TAIL_BYTES);
                long lastPing = System.currentTimeMillis();
                while (stream.open()) {
                    StringBuilder batch = new StringBuilder();
                    String line;
                    int count = 0;
                    while (count < 500 && (line = logs.readNewLine()) != null) {
                        if (count++ > 0) batch.append('\n');
                        batch.append(line);
                    }
                    if (count > 0) stream.send("lines", batch.toString());
                    if (PocketCloud.instance().servers().get(name).isEmpty() && !logs.awaitingEnd()) {
                        stream.send("end", "");
                        break;
                    }
                    if (System.currentTimeMillis() - lastPing > 15_000) {
                        stream.ping();
                        lastPing = System.currentTimeMillis();
                    }
                    if (count == 0) Thread.sleep(250);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                if (stream.open()) stream.send("failure", String.valueOf(e.getMessage()));
            } finally {
                logs.stopStream();
                stream.close();
            }
        });
    }

    /** Live console of this cloud process: event {@code lines} with new lines (ANSI colours), the last 1000 lines first. */
    @GetRoute("/panel-api/console")
    public void cloudConsole(HttpRequest request, HttpResponse response) {
        HttpResponse.EventStream stream = response.eventStream();
        Thread.ofVirtual().name("panel-cloud-console").start(() -> {
            long seq = 0, lastPing = System.currentTimeMillis();
            try {
                while (stream.open()) {
                    List<ConsoleTail.Line> lines = ConsoleTail.after(seq);
                    if (!lines.isEmpty()) {
                        seq = lines.getLast().seq();
                        stream.send("lines", String.join("\n", lines.stream().map(ConsoleTail.Line::text).toList()));
                    } else if (System.currentTimeMillis() - lastPing > 15_000) {
                        stream.ping();
                        lastPing = System.currentTimeMillis();
                    }
                    Thread.sleep(250);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                stream.close();
            }
        });
    }

    /** Runs a cloud command as if typed into the console of this node; the output shows up in the console stream. */
    @PostRoute("/panel-api/console")
    public void cloudCommand(HttpRequest request, HttpResponse response) {
        JsonObject body = request.json();
        String line = body != null && body.has("command") ? body.get("command").getAsString().strip() : "";
        if (line.isEmpty()) throw new HttpException(400, "Missing field: command");
        if (line.contains("\n") || line.contains("\r")) throw new HttpException(400, "One command per request.");
        CloudLogger.get().info("§8[§bPanel§8] §r> {}", line);
        PocketCloud.instance().console().submit(line);
        response.noContent();
    }
}
