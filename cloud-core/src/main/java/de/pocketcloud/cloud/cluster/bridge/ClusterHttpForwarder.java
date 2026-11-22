package de.pocketcloud.cloud.cluster.bridge;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.Peer;
import de.pocketcloud.cloud.cluster.bridge.packet.NodeHttpForwardPacket;
import de.pocketcloud.cloud.cluster.bridge.packet.NodeHttpForwardResultPacket;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.server.CloudServer;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the HTTP API on every node:
 * <ul>
 *     <li>reads are answered from the local cache,</li>
 *     <li>writes with cluster effect (server starts, templates, groups, maintenance) go to the head,</li>
 *     <li>requests about a server of another node (logs, command, save, stop) go to that node.</li>
 * </ul>
 * The response carries {@value #NODE_HEADER} with the node that processed the request.
 */
public final class ClusterHttpForwarder {

    public static final String NODE_HEADER = "X-PocketCloud-Node";
    /** Below the timeout of the request on the forwarding node (10 s). */
    private static final long TIMEOUT_MS = 8_000;

    private static final Pattern SERVER_ROUTE = Pattern.compile("^.*/v\\d+/servers/([^/]+)/(logs|dispatch|save|stop)$");
    private static final Pattern HEAD_ROUTE = Pattern.compile("^.*/v\\d+/(servers/start|templates(/.*)?|groups(/.*)?|maintenance(/.*)?|nodes/[^/]+/(drain|undrain))$");
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS", "QUERY");
    private static final Set<String> SKIPPED_HEADERS = Set.of("content-type", "content-length", "connection", "x-api-version");

    private final BridgeClusterModule module;

    ClusterHttpForwarder(BridgeClusterModule module) {
        this.module = module;
    }

    /**
     * Called by the router for an authenticated request. Returns true if the request was forwarded
     * (the response is then sent once the other node answers).
     */
    public static boolean tryForward(HttpRequest request, HttpResponse response) {
        if (request.clusterForwarded()) return false;
        return BridgeClusterModule.get().map(m -> m.http().forward(request, response)).orElse(false);
    }

    private boolean forward(HttpRequest request, HttpResponse response) {
        ClusterManager cluster = module.cluster();
        String path = request.path();
        String method = request.method().name();

        Matcher server = SERVER_ROUTE.matcher(path);
        if (server.matches()) {
            ICloudServer target = PocketCloud.instance().servers().get(server.group(1)).orElse(null);
            if (target instanceof CloudServer cs && cs.remote()) {
                send(cs.nodeName(), request, response);
                return true;
            }
            return false;
        }

        if (!READ_METHODS.contains(method) && HEAD_ROUTE.matcher(path).matches() && !cluster.isHead()) {
            String head = cluster.election().head();
            if (head == null || head.isEmpty()) throw new HttpException(503, "There is no head node right now.");
            send(head, request, response);
            return true;
        }

        return false;
    }

    private void send(String node, HttpRequest request, HttpResponse response) {
        ClusterManager cluster = module.cluster();
        Peer peer = cluster.peer(node).orElse(null);
        if (peer == null || !peer.connected()) throw new HttpException(503, "Node " + node + " is not reachable.");

        String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        NodeHttpForwardPacket packet = new NodeHttpForwardPacket(request.method().name(), request.uri(), contentType, request.body());
        packet.then(result -> {
            if (!(result instanceof NodeHttpForwardResultPacket r)) {
                sendError(response, 502, "Unexpected answer from node " + node + ".");
                return;
            }
            Map<String, Object> headers = r.getHeaders() == null ? Map.of() : r.getHeaders();
            String type = "application/json";
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                if (header.getKey().equalsIgnoreCase("content-type")) {
                    type = String.valueOf(header.getValue()).split(";")[0].trim();
                    continue;
                }
                if (SKIPPED_HEADERS.contains(header.getKey().toLowerCase())) continue;
                response.header(header.getKey(), String.valueOf(header.getValue()));
            }
            response.header(NODE_HEADER, node);
            response.status(r.getStatus());
            response.bytes(type, r.getBody() == null ? new byte[0] : r.getBody().getBytes(CharsetUtil.UTF_8));
        }).failure((_, e, reason) -> sendError(response, 504, "Node " + node + " did not answer: " + (e != null ? e.getMessage() : reason)));

        if (!cluster.send(node, packet)) throw new HttpException(503, "Node " + node + " is not reachable.");
        CloudLogger.get().debug("Forwarded HTTP {} {} to node {}", request.method().name(), request.path(), node);
    }

    private static void sendError(HttpResponse response, int status, String message) {
        if (response.isSent()) return;
        response.error(HttpResponseStatus.valueOf(status), message);
    }

    // ------------------------------------------------------------------ receiving side

    void handleForwarded(String from, Channel channel, NodeHttpForwardPacket packet) {
        module.executor().execute(() -> {
            NodeHttpForwardResultPacket result;
            try {
                result = execute(packet);
                CloudLogger.get().info("Handled HTTP §b{} {} §rforwarded by node §b{} §8(§b{}§8)§r.", packet.getMethod(), packet.getUri(), from, result.getStatus());
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to handle the HTTP request forwarded by node {}", e, from);
                result = new NodeHttpForwardResultPacket(500, Map.of("Content-Type", "application/json"),
                        "{\"error\":\"Internal Server Error\",\"message\":\"" + e.getClass().getSimpleName() + "\"}");
            }
            module.cluster().respond(channel, packet, result);
        });
    }

    private NodeHttpForwardResultPacket execute(NodeHttpForwardPacket packet) throws Exception {
        CompletableFuture<FullHttpResponse> written = new CompletableFuture<>();
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                if (msg instanceof FullHttpResponse r && !written.isDone()) {
                    written.complete(r.retainedDuplicate());
                }
                ReferenceCountUtil.release(msg);
                promise.setSuccess();
            }
        }, new ChannelInboundHandlerAdapter());

        try {
            byte[] body = packet.getBody() == null ? new byte[0] : packet.getBody().getBytes(CharsetUtil.UTF_8);
            FullHttpRequest raw = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(packet.getMethod()), packet.getUri(), Unpooled.wrappedBuffer(body));
            if (packet.getContentType() != null) raw.headers().set(HttpHeaderNames.CONTENT_TYPE, packet.getContentType());
            raw.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);

            HttpRequest request = new HttpRequest(raw, channel);
            request.clusterForwarded(true);
            // The response is written from the last handler, so it passes the capturing handler above.
            HttpResponse response = new HttpResponse(channel.pipeline().lastContext()).keepAlive(true);
            response.header(NODE_HEADER, module.cluster().nodeName());
            try {
                PocketCloud.instance().httpServer().router().handle(request, response);
            } catch (HttpException e) {
                if (!response.isSent()) response.error(HttpResponseStatus.valueOf(e.statusCode()), e.getMessage());
            }

            FullHttpResponse result = written.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            try {
                Map<String, Object> headers = new HashMap<>();
                result.headers().forEach(h -> headers.put(h.getKey(), h.getValue()));
                String content = result.content().toString(CharsetUtil.UTF_8);
                if (content.length() > BridgeClusterModule.MAX_TEXT_CHARS) {
                    content = content.substring(content.length() - BridgeClusterModule.MAX_TEXT_CHARS);
                }
                return new NodeHttpForwardResultPacket(result.status().code(), headers, content);
            } finally {
                result.release();
                raw.release();
            }
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
