package de.pocketcloud.cloud.server.start;

import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.util.concurrent.Promise;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public interface ServerStartMethod {

    Promise<Map<String, Long>> start(CloudServer[] servers);

    Promise<Optional<Long>> lookupPid(@NotNull CloudServer server);

    boolean isAvailable();
}