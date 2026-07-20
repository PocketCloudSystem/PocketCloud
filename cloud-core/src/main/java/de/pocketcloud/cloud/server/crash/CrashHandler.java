package de.pocketcloud.cloud.server.crash;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.server.CloudServer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CrashHandler {

    @Nullable CrashData retrieveCrashData(CloudServer server);

    List<IServerSoftware> applicableSoftware();
}