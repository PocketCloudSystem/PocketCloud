package de.pocketcloud.cloud.http.panel;

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

    @PostRoute(value = "/panel-api/login", authentication = NonRequiredAuthentication.class)
    public void login(HttpRequest request, HttpResponse response) {
        String address = request.remoteAddress();
        if (!PanelSessions.mayTry(address)) throw new HttpException(429, "Too many failed logins, try again in a minute.");
        String key = null;
        try {
            JsonObject body = request.json();
            if (body != null && body.has("key")) key = body.get("key").getAsString().trim();
        } catch (RuntimeException ignored) {
            // no JSON body
        }
        if (key == null || !StringUtils.secureEquals(key, PocketCloud.instance().httpServer().authToken())) {
            PanelSessions.failed(address);
            throw new HttpException(401, "Wrong auth-key.");
        }
        PanelSessions.succeeded(address);
        String prefix = PocketCloud.instance().httpServer().router().routePrefix();
        response.cookie(PanelSessions.COOKIE, PanelSessions.create(), c -> c.path(prefix.isEmpty() ? "/" : prefix)
                .httpOnly(true).sameSite(CookieBuilder.SameSite.STRICT).maxAge(PanelSessions.LIFETIME.toSeconds())
                .secure(PocketCloud.instance().config().httpServer().ssl().enabled()));
        response.json(this::info);
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
        response.json(this::info);
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

    private void info(JsonObject json) {
        var cloud = PocketCloud.instance();
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
}
