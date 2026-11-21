package de.pocketcloud.cloud.server.util;

import de.pocketcloud.cloud.cluster.bridge.BridgeClusterModule;
import de.pocketcloud.cloud.cluster.bridge.packet.NodeServerActionPacket;
import de.pocketcloud.cloud.server.CloudServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ServerLogStream {

    private final CloudServer server;
    private RandomAccessFile fileHandle;
    private boolean startedStream = false;

    private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

    // Servers of another node: the log is polled from the owning node.
    private boolean remote = false;
    private final Queue<String> remoteLines = new ConcurrentLinkedQueue<>();
    private volatile int nextRemoteLine = -100;
    private volatile boolean fetching = false;
    private volatile long lastFetch = 0;

    public ServerLogStream(CloudServer server) {
        this.server = server;
    }

    public void startStream() {
        if (server.remote()) {
            if (BridgeClusterModule.get().isEmpty()) throw new RuntimeException("Node " + server.nodeName() + " is not reachable");
            remote = true;
            startedStream = true;
            fetchRemote();
            return;
        }

        File logFile = server.logFilePath().toFile();
        if (!logFile.exists()) throw new RuntimeException("Log file does not exist");
        try {
            this.fileHandle = new RandomAccessFile(logFile, "r");
            this.startedStream = true;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Log file cannot be opened", e);
        }
    }

    public String readNewLine() {
        if (!startedStream) return null;
        if (remote) {
            String line = remoteLines.poll();
            if (line == null) fetchRemote();
            return line;
        }
        try {
            int b;
            while ((b = fileHandle.read()) != -1) {
                if (b == '\n') {
                    return flushLine();
                } else if (b == '\r') {
                    long pos = fileHandle.getFilePointer();
                    int next = fileHandle.read();
                    if (next != '\n' && next != -1) {
                        fileHandle.seek(pos);
                    }
                    return flushLine();
                } else {
                    lineBuffer.write(b);
                }
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private void fetchRemote() {
        long now = System.currentTimeMillis();
        if (fetching || now - lastFetch < 1000) return;
        BridgeClusterModule module = BridgeClusterModule.get().orElse(null);
        if (module == null) return;
        fetching = true;
        lastFetch = now;
        module.requestAction(server, NodeServerActionPacket.Action.LOGS, String.valueOf(nextRemoteLine), null)
                .thenSuccess(result -> {
                    if (result.isSuccess() && startedStream) {
                        if (result.getLines() != null) remoteLines.addAll(result.getLines());
                        if (result.getData() != null && result.getData().get("total") instanceof Number total) nextRemoteLine = total.intValue();
                    }
                    fetching = false;
                })
                .failure(_ -> fetching = false);
    }

    private String flushLine() {
        String line = lineBuffer.toString(StandardCharsets.UTF_8);
        lineBuffer.reset();
        return line;
    }

    public void stopStream() {
        if (startedStream && fileHandle != null) {
            try {
                fileHandle.close();
            } catch (IOException _) {}
        }

        startedStream = false;
        remote = false;
        remoteLines.clear();
        fileHandle = null;
        lineBuffer.reset();
    }
}