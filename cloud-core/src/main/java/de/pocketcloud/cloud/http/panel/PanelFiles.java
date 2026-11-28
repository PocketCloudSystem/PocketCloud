package de.pocketcloud.cloud.http.panel;

import de.pocketcloud.cloud.http.io.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Serves the web panel (built into the jar under {@code /panel/}) at {@code <routePrefix>/panel/}. Unknown paths
 * without a file extension get index.html, so the panel's own routes survive a reload.
 */
public final class PanelFiles {

    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html", "js", "text/javascript", "css", "text/css", "svg", "image/svg+xml",
            "png", "image/png", "ico", "image/x-icon", "json", "application/json", "woff2", "font/woff2");

    private PanelFiles() {
    }

    /** @return false if the path is not part of the panel */
    public static boolean serve(String routePrefix, String path, HttpResponse response) {
        String base = routePrefix + "/panel";
        if (path.equals(base)) {
            response.redirect(base + "/");
            return true;
        }
        if (!path.startsWith(base + "/")) return false;

        String file = path.substring(base.length() + 1);
        if (file.isEmpty() || file.contains("..") || file.contains("\\")) file = "index.html";
        byte[] data = read(file);
        String name = file.substring(file.lastIndexOf('/') + 1);
        if (data == null && !name.contains(".")) {
            file = "index.html";
            data = read(file);
        }
        if (data == null) {
            if (read("index.html") == null) {
                response.status(404).text("The web panel is not part of this build (built with -DskipPanel).");
            } else {
                response.status(404).text("404 Not Found");
            }
            return true;
        }

        String extension = file.substring(file.lastIndexOf('.') + 1);
        // vite puts a content hash into the names under assets/, so they never change
        response.header("Cache-Control", file.startsWith("assets/") ? "public, max-age=31536000, immutable" : "no-cache");
        response.header("X-Content-Type-Options", "nosniff");
        if (file.equals("index.html")) {
            response.header("X-Frame-Options", "DENY");
            response.header("Referrer-Policy", "no-referrer");
        }
        response.bytes(TYPES.getOrDefault(extension, "application/octet-stream"), data);
        return true;
    }

    private static byte[] read(String file) {
        try (InputStream in = PanelFiles.class.getResourceAsStream("/panel/" + file)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}
