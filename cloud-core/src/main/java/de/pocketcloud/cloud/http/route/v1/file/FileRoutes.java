package de.pocketcloud.cloud.http.route.v1.file;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.http.annotation.*;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.util.PocketCloudPaths;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Files of templates (read/write, replicated in a cluster like any local change), of running servers (read only,
 * answered by the node that runs the server) and crash reports of the answering node.
 */
@ApiVersion(1)
public final class FileRoutes {

    /** Bigger files are not served or accepted (the HTTP server aggregates at most 10 MB per request). */
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    // ------------------------------------------------------------------ templates

    @GetRoute("/templates/{name}/files")
    public void templateRead(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        read(templateRoot(name), request.queryParam("path", ""), response);
    }

    @PutRoute("/templates/{name}/files")
    public void templateWrite(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        Path file = resolve(templateRoot(name), request.queryParam("path", ""));
        if (file.equals(templateRoot(name)) || Files.isDirectory(file)) throw new HttpException(400, "Not a file path.");
        byte[] data = request.bodyBytes();
        if (data.length > MAX_FILE_BYTES) throw new HttpException(413, "Files over 10 MB cannot be uploaded here.");
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("." + file.getFileName() + ".panel-tmp");
            Files.write(tmp, data);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new HttpException(500, "Could not write the file: " + e.getMessage());
        }
        response.json(message("Saved " + relative(templateRoot(name), file) + "."));
    }

    @PostRoute("/templates/{name}/files/mkdir")
    public void templateMkdir(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        Path dir = resolve(templateRoot(name), request.queryParam("path", ""));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new HttpException(500, "Could not create the folder: " + e.getMessage());
        }
        response.json(message("Created " + relative(templateRoot(name), dir) + "."));
    }

    @DeleteRoute("/templates/{name}/files")
    public void templateDelete(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        Path root = templateRoot(name);
        Path target = resolve(root, request.queryParam("path", ""));
        if (target.equals(root)) throw new HttpException(400, "The template folder itself cannot be deleted here.");
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new HttpException(404, "No such file.");
        try (Stream<Path> walk = Files.walk(target)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        } catch (IOException e) {
            throw new HttpException(500, "Could not delete: " + e.getMessage());
        }
        response.json(message("Deleted " + relative(root, target) + "."));
    }

    // ------------------------------------------------------------------ servers and crashes

    @GetRoute("/servers/{name}/files")
    public void serverRead(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        if (!(PocketCloud.instance().servers().get(name).orElse(null) instanceof CloudServer server))
            throw new HttpException(404, "Server not found.");
        if (server.remote()) throw new HttpException(503, "The node of " + name + " did not take the request.");
        read(server.path(), request.queryParam("path", ""), response);
    }

    /** Crash reports of this node (in a cluster: of the servers that ran here). */
    @GetRoute("/crashes")
    public void crashes(HttpRequest request, HttpResponse response) {
        read(PocketCloudPaths.storage().crashes().server().asPath(), request.queryParam("path", ""), response);
    }

    // ------------------------------------------------------------------ helpers

    private static Path templateRoot(String name) {
        var template = PocketCloud.instance().templates().get(name).orElseThrow(() -> new HttpException(404, "Template not found."));
        return ((de.pocketcloud.cloud.template.Template) template).path().toAbsolutePath().normalize();
    }

    /** Resolves a client path inside root; anything leaving it (.., absolute paths, symlinks) is refused. */
    static Path resolve(Path root, String relative) {
        Path base = root.toAbsolutePath().normalize();
        String clean = relative == null ? "" : relative.replace('\\', '/').replaceAll("^/+", "");
        Path path = base.resolve(clean).normalize();
        if (!path.startsWith(base)) throw new HttpException(400, "Path outside of the folder.");
        try {
            if (Files.exists(path) && Files.exists(base) && !path.toRealPath().startsWith(base.toRealPath()))
                throw new HttpException(400, "Path outside of the folder.");
        } catch (IOException e) {
            throw new HttpException(400, "Invalid path.");
        }
        return path;
    }

    private static void read(Path root, String relative, HttpResponse response) {
        Path path = resolve(root, relative);
        try {
            if (Files.isDirectory(path) || path.equals(root.toAbsolutePath().normalize())) {
                JsonArray entries = new JsonArray();
                if (Files.isDirectory(path)) {
                    try (Stream<Path> list = Files.list(path)) {
                        for (Path p : list.sorted(Comparator.comparing((Path x) -> !Files.isDirectory(x)).thenComparing(x -> x.getFileName().toString().toLowerCase())).toList()) {
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            JsonObject entry = new JsonObject();
                            entry.addProperty("name", p.getFileName().toString());
                            entry.addProperty("dir", attrs.isDirectory());
                            entry.addProperty("size", attrs.isDirectory() ? 0 : attrs.size());
                            entry.addProperty("modified", attrs.lastModifiedTime().toMillis());
                            entries.add(entry);
                        }
                    }
                }
                JsonObject json = new JsonObject();
                json.addProperty("path", relative(root, path));
                json.add("entries", entries);
                response.json(json);
                return;
            }
            if (!Files.isRegularFile(path)) throw new HttpException(404, "No such file.");
            if (Files.size(path) > MAX_FILE_BYTES) throw new HttpException(413, "The file is over 10 MB.");
            response.header("Content-Disposition", "inline; filename=\"" + path.getFileName().toString().replace("\"", "") + "\"");
            response.bytes("application/octet-stream", Files.readAllBytes(path));
        } catch (IOException e) {
            throw new HttpException(500, "Could not read: " + e.getMessage());
        }
    }

    private static String relative(Path root, Path path) {
        return root.toAbsolutePath().normalize().relativize(path).toString().replace('\\', '/');
    }

    private static JsonObject message(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        return json;
    }
}
