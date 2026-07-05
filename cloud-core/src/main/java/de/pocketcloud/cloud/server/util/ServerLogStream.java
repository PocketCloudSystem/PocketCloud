package de.pocketcloud.cloud.server.util;

import de.pocketcloud.cloud.server.CloudServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class ServerLogStream {

    private final CloudServer server;
    private RandomAccessFile fileHandle;
    private boolean startedStream = false;

    public ServerLogStream(CloudServer server) {
        this.server = server;
    }

    public void startStream() {
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
        try {
            String line = fileHandle.readLine();
            if (line == null) {
                long currentPos = fileHandle.getFilePointer();
                fileHandle.seek(currentPos);
                line = fileHandle.readLine();
                if (line == null) return null;
            }

            return line.trim();
        } catch (IOException e) {
            return null;
        }
    }

    public void stopStream() {
        if (startedStream && fileHandle != null) {
            try {
                fileHandle.close();
            } catch (IOException _) {}
        }

        startedStream = false;
        fileHandle = null;
    }
}