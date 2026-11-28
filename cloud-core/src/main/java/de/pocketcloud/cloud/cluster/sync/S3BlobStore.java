package de.pocketcloud.cloud.cluster.sync;

import de.pocketcloud.cloud.config.sub.S3Configuration;
import de.pocketcloud.cloud.console.log.CloudLogger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Template files in an S3 bucket (AWS, MinIO, Cloudflare R2, ...), stored once under their SHA-256:
 * {@code <prefix>blobs/<sha256>}. The head uploads the files of every new version, the other nodes download them
 * from the bucket instead of from the head. Versions, manifests and conflicts still go through the cluster
 * connection; if the bucket fails, the nodes fall back to fetching from the head.
 * <p>
 * Signed with AWS Signature V4 using path-style URLs ({@code endpoint/bucket/key}).
 * shortcut: single PUT per file (S3 limit 5 GB per file), switch to multipart upload if templates get files that large.
 */
public final class S3BlobStore {

    private static final String UNSIGNED = "UNSIGNED-PAYLOAD";
    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String endpoint;
    private final String region;
    private final String bucket;
    private final String prefix;
    private final String accessKey;
    private final String secretKey;
    /** Hashes known to be in the bucket (uploaded or found by this node). */
    private final Set<String> known = ConcurrentHashMap.newKeySet();

    S3BlobStore(String endpoint, String region, String bucket, String prefix, String accessKey, String secretKey) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.region = region;
        this.bucket = bucket;
        this.prefix = prefix;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * The store of {@code cluster.s3} in config.yml, or null if the template storage is "packets" or the
     * credentials are missing. Credentials: {@code credentialsFile} (accessKey=, secretKey=), otherwise the
     * environment variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY.
     */
    static S3BlobStore fromConfig(String templateStorage, S3Configuration config, Path credentialsFile) {
        if (!"s3".equals(templateStorage)) return null;
        String access = System.getenv("AWS_ACCESS_KEY_ID");
        String secret = System.getenv("AWS_SECRET_ACCESS_KEY");
        if (Files.isRegularFile(credentialsFile)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(credentialsFile)) {
                p.load(in);
            } catch (IOException e) {
                CloudLogger.get().warn("Could not read §b{}§r: {}", credentialsFile, e.getMessage());
            }
            access = p.getProperty("accessKey", access);
            secret = p.getProperty("secretKey", secret);
        }
        if (access == null || access.isBlank() || secret == null || secret.isBlank()) {
            CloudLogger.get().warn("S3 template storage is enabled, but there are no S3 credentials in §b{} §ror the environment. Using the cluster connection instead.", credentialsFile);
            return null;
        }
        return new S3BlobStore(config.endpoint(), config.region(), config.bucket(), config.prefix(), access.trim(), secret.trim());
    }

    String describe() {
        return endpoint + "/" + bucket + "/" + prefix;
    }

    /** Uploads the file unless the bucket already has it. {@code sha} must be the file's SHA-256. */
    void upload(String sha, Path file) throws IOException, InterruptedException {
        if (known.contains(sha)) return;
        if (send("HEAD", sha, null, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
            known.add(sha);
            return;
        }
        HttpResponse<String> response = send("PUT", sha, file, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("S3 upload failed (" + response.statusCode() + "): " + shorten(response.body()));
        known.add(sha);
    }

    /** Downloads the file with this SHA-256 to {@code target} and verifies it; false if the bucket does not have it. */
    boolean download(String sha, Path target) throws IOException, InterruptedException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".s3");
        try {
            HttpResponse<Path> response = send("GET", sha, null, HttpResponse.BodyHandlers.ofFile(tmp));
            if (response.statusCode() == 404) return false;
            if (response.statusCode() != 200) throw new IOException("S3 download failed (" + response.statusCode() + "): " + shorten(Files.readString(tmp)));
            if (!UnitScanner.sha256(tmp).equals(sha)) throw new IOException("S3 object " + sha + " has a different hash");
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            known.add(sha);
            return true;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private <T> HttpResponse<T> send(String method, String sha, Path body, HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        String path = "/" + bucket + "/" + prefix + "blobs/" + sha;
        URI uri = URI.create(endpoint + path);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String payloadHash = body == null ? UNSIGNED : sha; // the blob key is the SHA-256 of its content
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(10))
                .header("x-amz-content-sha256", payloadHash)
                .header("x-amz-date", AMZ_DATE.format(now))
                .header("Authorization", authorization(method, uri, path, payloadHash, now))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofFile(body));
        return http.send(request.build(), handler);
    }

    /** AWS Signature Version 4 for a request without query string, signing host, x-amz-content-sha256 and x-amz-date. */
    String authorization(String method, URI uri, String path, String payloadHash, ZonedDateTime now) {
        String amzDate = AMZ_DATE.format(now);
        String date = amzDate.substring(0, 8);
        String host = uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
        String signedHeaders = "host;x-amz-content-sha256;x-amz-date";
        String canonical = method + "\n" + path + "\n\n"
                + "host:" + host + "\n" + "x-amz-content-sha256:" + payloadHash + "\n" + "x-amz-date:" + amzDate + "\n\n"
                + signedHeaders + "\n" + payloadHash;
        String scope = date + "/" + region + "/s3/aws4_request";
        String toSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + scope + "\n" + hex(sha256(canonical));
        byte[] key = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        key = hmac(key, region);
        key = hmac(key, "s3");
        key = hmac(key, "aws4_request");
        return "AWS4-HMAC-SHA256 Credential=" + accessKey + "/" + scope + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + hex(hmac(key, toSign));
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] sha256(String data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    private static String shorten(String body) {
        return body == null ? "" : body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}
