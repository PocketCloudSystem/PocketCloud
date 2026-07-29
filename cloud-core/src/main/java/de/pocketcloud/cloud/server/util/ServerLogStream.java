package de.pocketcloud.cloud.server.util;

import de.pocketcloud.cloud.server.CloudServer;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class ServerLogStream {

    private final CloudServer server;
    private RandomAccessFile fileHandle;
    private boolean startedStream = false;

    private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

    public ServerLogStream(CloudServer server) {
        this.server = server;
    }

    public void startStream() {
        File logFile = server.customLogFilePath().toFile().exists() ? server.customLogFilePath().toFile() : server.logFilePath().toFile();
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
        fileHandle = null;
        lineBuffer.reset();
    }
}