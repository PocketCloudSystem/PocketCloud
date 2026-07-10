package de.pocketcloud.cloud.server.crash;

import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CrashHandler {

    @Nullable CrashData retrieveCrashData(CloudServer server);

    List<ServerSoftware> applicableSoftware();
}