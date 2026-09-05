package de.pocketcloud.cloud.server.start;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.shared.component.software.ServerSoftware;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static de.pocketcloud.cloud.server.CloudServerManager.SERVER_EXECUTOR;

public final class ProcessServerStartMethod implements ServerStartMethod {

    @Override
    public Promise<Map<String, Long>> start(CloudServer[] servers) {
        return Promise.supplyAsync(() -> {
            Map<String, Long> pids = new HashMap<>();
            for (CloudServer server : servers) {
                ServerSoftware software = (ServerSoftware) server.template().serverSoftware();
                String startCommand = prepareStartCommand(software, server);

                try {
                    CloudLogger.get().debug("Starting {} with {}", server.name(), startCommand);

                    Process process = new ProcessBuilder(tokenizeCommand(startCommand))
                            .redirectErrorStream(true)
                            .directory(server.path().toFile())
                            .start();

                    if (!process.isAlive() && process.exitValue() != 0) {
                        CloudLogger.get().info(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                        throw new RuntimeException("Failed to start server (" + process.exitValue() + "): " + startCommand);
                    }

                    pids.put(server.name(), process.pid());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to start server", e);
                }
            }

            return pids;
        }, SERVER_EXECUTOR);
    }

    private List<String> tokenizeCommand(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (inQuotes) {
                if (c == quoteChar) {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
                continue;
            }

            if (c == '"' || c == '\'') {
                inQuotes = true;
                quoteChar = c;
            } else if (Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) tokens.add(current.toString());
        if (inQuotes) throw new IllegalArgumentException("Unbalanced quote in command: " + command);
        return tokens;
    }

    @Override
    public Promise<Optional<Long>> lookupPid(@NotNull CloudServer server) {
        return Promise.resolved(Optional.empty());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}