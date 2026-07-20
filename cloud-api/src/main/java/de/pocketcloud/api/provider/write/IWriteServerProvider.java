package de.pocketcloud.api.provider.write;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.provider.IServerProvider;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface IWriteServerProvider extends IServerProvider {

    void add(ICloudServer server);

    void remove(ICloudServer server);
}