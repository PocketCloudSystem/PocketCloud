package de.pocketcloud.cloud.server.start;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.shared.component.software.ServerSoftware;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public interface ServerStartMethod {

    Promise<Map<String, Long>> start(CloudServer[] servers);

    Promise<Optional<Long>> lookupPid(@NotNull CloudServer server);

    boolean isAvailable();

    default String prepareStartCommand(ServerSoftware software, CloudServer server) {
        return software.download().realStartCommand()
                .replace("{BINARY_PATH}", quote(PocketCloud.instance().software().binaryDirectoryPath(software).toAbsolutePath() + File.separator))
                .replace("{SOFTWARE_PATH}", quote(PocketCloud.instance().software().directoryPath(software).toAbsolutePath() + File.separator))
                .replace("{MAX_MEMORY}", String.valueOf(server.template().settings().maxMemory()));
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }
}