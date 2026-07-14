package de.pocketcloud.cloud.server.start;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.common.concurrent.Promise;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.pocketcloud.cloud.server.CloudServerManager.SERVER_EXECUTOR;

public final class ProcessServerStartMethod implements ServerStartMethod {

    @Override
    public Promise<Map<String, Long>> start(CloudServer[] servers) {
        return Promise.supplyAsync(() -> {
            Map<String, Long> pids = new HashMap<>();
            for (CloudServer server : servers) {
                ServerSoftware software = server.template().serverSoftware();
                String startCommand = software.download().startCommand()
                        .replace("{BINARY_PATH}", software.binary().directoryPath().toAbsolutePath() + File.separator)
                        .replace("{SOFTWARE_PATH}", software.directoryPath().toAbsolutePath() + File.separator);

                try {
                    Process process = new ProcessBuilder(startCommand.split(" "))
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

    @Override
    public Promise<Optional<Long>> lookupPid(@NotNull CloudServer server) {
        return Promise.resolved(Optional.empty());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}