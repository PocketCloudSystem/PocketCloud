package de.pocketcloud.cloud.server.start;

import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.util.TerminalUtils;
import de.pocketcloud.cloud.util.concurrent.Promise;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static de.pocketcloud.cloud.server.CloudServerManager.SERVER_EXECUTOR;

public final class ScreenServerStartMethod implements ServerStartMethod {

    @Override
    public Promise<Map<String, Long>> start(CloudServer[] servers) {
        return Promise.supplyAsync(() -> {
            Map<String, Long> map = new HashMap<>();
            List<String> commands = new ArrayList<>();

            for (CloudServer server : servers) {
                String paneName = server.name() + "-" + server.uuid().toString();
                ServerSoftware software = server.template().serverSoftware();
                String startCommand = software.download().startCommand()
                        .replace("{BINARY_PATH}", software.binary().directoryPath().toAbsolutePath() + File.separator)
                        .replace("{SOFTWARE_PATH}", software.directoryPath().toAbsolutePath() + File.separator);

                commands.add("cd " + TerminalUtils.shellEscape(server.path().toAbsolutePath().toString()) +
                        " " +
                        "&&" +
                        "screen -dmS " + TerminalUtils.shellEscape(paneName) +
                        " " +
                        "bash -lc " + TerminalUtils.shellEscape("exec " + startCommand));
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
            String screenName = server.name() + "-" + server.uuid().toString();
            String command = "SCREEN_PID=$(screen -ls " + TerminalUtils.shellEscape(screenName) + " 2>/dev/null | grep -oP '^\\s*\\K[0-9]+(?=\\." + screenName + ")' | head -n1) ; " + "pgrep -P $SCREEN_PID | head -n1";

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
        return TerminalUtils.isInstalled("screen");
    }
}