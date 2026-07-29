package de.pocketcloud.cloud.server.start;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.common.util.TerminalUtils;
import de.pocketcloud.shared.component.software.ServerSoftware;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static de.pocketcloud.cloud.server.CloudServerManager.SERVER_EXECUTOR;

public final class TmuxServerStartMethod implements ServerStartMethod {

    @Override
    public Promise<Map<String, Long>> start(CloudServer[] servers) {
        return Promise.supplyAsync(() -> {
            Map<String, Long> map = new HashMap<>();
            List<String> commands = new ArrayList<>();

            for (CloudServer server : servers) {
                String paneName = server.name() + "-" + server.uuid().toString();
                ServerSoftware software = (ServerSoftware) server.template().serverSoftware();
                String startCommand = prepareStartCommand(software, server);
                Path loggingPath = server.customLogFilePath();

                commands.add(
                        "cd " + TerminalUtils.shellEscape(server.path().toAbsolutePath().toString()) +
                                " && " +
                                "tmux new-session -d -s " + TerminalUtils.shellEscape(paneName) +
                                " bash -lc " + TerminalUtils.shellEscape("exec " + startCommand) +
                                " && " +
                                "tmux pipe-pane -t " + TerminalUtils.shellEscape(paneName) +
                                " -o " +
                                TerminalUtils.shellEscape("cat >> " + loggingPath.toAbsolutePath())
                );

                CloudLogger.get().debug("Starting {} with {}", server.name(), commands.getLast());

                map.put(paneName, null);
            }

            try {
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", String.join(" ; ", commands)});
                if (!process.isAlive() && process.exitValue() != 0) {
                    throw new RuntimeException("Failed to start servers: " + commands);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return map;
        }, SERVER_EXECUTOR);
    }

    @Override
    public Promise<Optional<Long>> lookupPid(@NotNull CloudServer server) {
        return Promise.supplyAsync(() -> {
            String paneName = server.name() + "-" + server.uuid().toString();
            String command = "tmux list-panes -t " + TerminalUtils.shellEscape(paneName) + " -F '#{pane_pid}' 2>/dev/null | head -n1";

            try {
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
                String output = new String(process.getInputStream().readAllBytes()).trim();
                if (output.isEmpty()) return Optional.empty();
                return Optional.of(Long.parseLong(output));
            } catch (IOException | NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return TerminalUtils.isInstalled("tmux");
    }
}