package de.pocketcloud.cloud.util.net;

import de.pocketcloud.cloud.util.FormatUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NetUtils {

    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes\\s+\\d+-\\d+/(\\d+)");

    public static boolean isLocalUdpPortFree(int port) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isValidUrl(String url) {
        try {
            URI.create(url).toURL();
            return true;
        } catch (MalformedURLException | IllegalArgumentException e) {
            return false;
        }
    }

    public static long downloadSize(String url) {
        long headReqSize = tryHeadRequest(url);
        if (headReqSize != -1) {
            return headReqSize;
        }

        return tryRangeRequest(url);
    }

    private static long tryHeadRequest(String url) {
        try (HttpClient client = createClient()) {
            return client.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .method("HEAD", HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.discarding()
            ).headers().firstValueAsLong("content-length").orElse(-1L);
        } catch (Exception e) {
            return -1L;
        }
    }

    private static long tryRangeRequest(String url) {
        try (HttpClient client = createClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Range", "bytes=0-0")
                    .GET()
                    .build();

            try {
                HttpResponse<Void> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.discarding()
                );

                List<String> contentRange = response.headers().allValues("Content-Range");

                for (String header : contentRange) {
                    Matcher m = CONTENT_RANGE.matcher(header);
                    if (m.find()) {
                        return Long.parseLong(m.group(1));
                    }
                }
            } catch (IOException | InterruptedException e) {
                return -1L;
            }

        } catch (Exception e) {
            return -1L;
        }

        return -1L;
    }

    public static void download(String url, Path targetFile, Consumer<DownloadProgress> onProgress) throws IOException, InterruptedException {
        Files.createDirectories(targetFile.getParent());

        try (HttpClient client = createClient()) {
            if (onProgress != null) onProgress.accept(new DownloadProgress(0, 0, 0, 0, 0));

            HttpResponse<InputStream> response = client.send(
                    HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            long totalBytes = response.headers()
                    .firstValueAsLong("content-length")
                    .orElse(-1L);

            try (InputStream in = response.body();
                 OutputStream out = Files.newOutputStream(targetFile)) {

                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int read;

                long startTime = System.currentTimeMillis();
                long lastTime = startTime;
                long lastBytes = 0;

                boolean firstUpdate = true;

                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    downloaded += read;

                    long now = System.currentTimeMillis();

                    if (onProgress != null && (firstUpdate || now - lastTime >= 500)) {
                        firstUpdate = false;
                        long elapsedSinceLastMs = now - lastTime;
                        long bytesSinceLast = downloaded - lastBytes;

                        double speedBytesPerSec = elapsedSinceLastMs > 0 ? bytesSinceLast / (elapsedSinceLastMs / 1000.0) : 0;
                        double percent = totalBytes > 0 ? (double) downloaded / totalBytes * 100 : -1;
                        long etaSeconds = (totalBytes > 0 && speedBytesPerSec > 0) ? (long) ((totalBytes - downloaded) / speedBytesPerSec) : -1;

                        onProgress.accept(new DownloadProgress(percent, downloaded, totalBytes, speedBytesPerSec, etaSeconds));

                        lastTime = now;
                        lastBytes = downloaded;
                    }
                }

                if (onProgress != null) {
                    long totalMs = System.currentTimeMillis() - startTime;
                    double avgSpeed = totalMs > 0 ? downloaded / (totalMs / 1000.0) : 0;
                    onProgress.accept(new DownloadProgress(100, downloaded, downloaded, avgSpeed, 0));
                }
            }
        }
    }

    public static void download(String url, Path targetFile) throws Exception {
        download(url, targetFile, null);
    }

    private static HttpClient createClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public record DownloadProgress(double percent, long downloadedBytes, long totalBytes, double speedBytesPerSec, long etaSeconds) {

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