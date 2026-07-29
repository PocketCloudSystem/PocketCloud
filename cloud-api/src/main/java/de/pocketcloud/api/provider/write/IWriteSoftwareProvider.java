package de.pocketcloud.api.provider.write;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.provider.ISoftwareProvider;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface IWriteSoftwareProvider extends ISoftwareProvider {

    default void register(IServerSoftware software) {
        register(software, false);
    }

    void register(IServerSoftware software, boolean override);

    void unregister(IServerSoftware software);
}