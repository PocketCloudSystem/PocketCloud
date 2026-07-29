package de.pocketcloud.cloud.server.crash;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.server.CloudServer;

import java.util.List;

public interface CrashHandler {

    CrashData retrieveCrashData(CloudServer server);

    List<IServerSoftware> applicableSoftware();
}