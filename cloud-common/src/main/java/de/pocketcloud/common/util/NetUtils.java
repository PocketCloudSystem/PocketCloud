
package de.pocketcloud.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NetUtils {

    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes\\s+\\d+-\\d+/(\\d+)");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private NetUtils() {}

    public static boolean isLocalUdpPortFree(int port) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isValidUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static long downloadSize(String url) {
        long size = tryHeadRequest(url);
        return size >= 0 ? size : tryRangeRequest(url);
    }

    private static long tryHeadRequest(String url) {
        try (HttpClient client = createClient()) {
            HttpResponse<Void> response = client.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(REQUEST_TIMEOUT)
                            .method("HEAD", HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.discarding()
            );

            if (response.statusCode() != 200) {
                return -1L;
            }

            return response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElse(-1L);
        } catch (IOException | IllegalArgumentException e) {
            return -1L;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1L;
        }
    }

    private static long tryRangeRequest(String url) {
        try (HttpClient client = createClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Range", "bytes=0-0")
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            if (response.statusCode() != 206) {
                return -1L;
            }

            List<String> headers = response.headers().allValues("Content-Range");
            for (String header : headers) {
                Matcher matcher = CONTENT_RANGE.matcher(header);
                if (matcher.find()) {
                    return Long.parseLong(matcher.group(1));
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            return -1L;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1L;
        }

        return -1L;
    }

    public static void download(String url, Path targetFile, Consumer<DownloadProgress> onProgress)
            throws IOException, InterruptedException {
        Path absoluteTarget = targetFile.toAbsolutePath();
        Path parent = absoluteTarget.getParent();

        if (parent == null) throw new IOException("Target has no parent directory: " + targetFile);

        Files.createDirectories(parent);

        // Same directory so an atomic move is possible on the same filesystem.
        Path tempFile = Files.createTempFile(parent, "download-", ".tmp");
        boolean moved = false;

        try {
            if (onProgress != null) {
                onProgress.accept(new DownloadProgress(0, 0, -1, 0, -1));
            }

            try (HttpClient client = createClient()) {
                HttpResponse<InputStream> response = client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(REQUEST_TIMEOUT)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofInputStream()
                );

                try (InputStream in = response.body()) {
                    if (response.statusCode() != 200) {
                        throw new IOException("Download failed: HTTP " + response.statusCode() + " for " + url);
                    }

                    long totalBytes = response.headers()
                            .firstValueAsLong("Content-Length")
                            .orElse(-1L);

                    long downloaded = 0;
                    long startTime = System.nanoTime();
                    long lastTime = startTime;
                    long lastBytes = 0;
                    boolean firstUpdate = true;

                    try (OutputStream out = Files.newOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int read;

                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                            downloaded += read;

                            long now = System.nanoTime();

                            if (onProgress != null && (firstUpdate || now - lastTime >= 500_000_000L)) {

                                firstUpdate = false;

                                double elapsedSeconds = (now - lastTime) / 1_000_000_000.0;
                                double speed = elapsedSeconds > 0 ? (downloaded - lastBytes) / elapsedSeconds : 0;

                                double percent = totalBytes > 0 ? (double) downloaded / totalBytes * 100 : -1;

                                long eta = totalBytes > 0 && speed > 0 ? Math.max(0, (long) ((totalBytes - downloaded) / speed)) : -1;

                                onProgress.accept(new DownloadProgress(
                                        percent, downloaded, totalBytes, speed, eta
                                ));

                                lastTime = now;
                                lastBytes = downloaded;
                            }
                        }
                    }

                    // Detect truncated downloads when Content-Length is provided.
                    if (totalBytes >= 0 && downloaded != totalBytes) {
                        throw new IOException("Incomplete download: expected " + totalBytes + " bytes, received " + downloaded);
                    }

                    long elapsedNanos = System.nanoTime() - startTime;
                    double avgSpeed = elapsedNanos > 0 ? downloaded / (elapsedNanos / 1_000_000_000.0) : 0;

                    if (onProgress != null) {
                        onProgress.accept(new DownloadProgress(100, downloaded, downloaded, avgSpeed, 0));
                    }
                }
            }

            try {
                Files.move(tempFile, absoluteTarget,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Keep the old file safe if atomic replacement is unavailable.
                throw new IOException("Atomic replacement is not supported for " + absoluteTarget, e);
            }

            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    public static void download(String url, Path targetFile)
            throws IOException, InterruptedException {
        download(url, targetFile, null);
    }

    private static HttpClient createClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    public record DownloadProgress(
            double percent,
            long downloadedBytes,
            long totalBytes,
            double speedBytesPerSec,
            long etaSeconds
    ) {

        public String formatSpeed() {
            return FormatUtils.downloadSpeed(speedBytesPerSec);
        }

        public String formatEta() {
            return FormatUtils.seconds(etaSeconds, 3);
        }

        public String formatSize(long bytes) {
            return FormatUtils.bytes(bytes, false);
        }
    }
}