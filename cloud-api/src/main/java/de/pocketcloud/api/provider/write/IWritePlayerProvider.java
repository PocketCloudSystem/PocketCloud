package de.pocketcloud.api.provider.write;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.provider.IPlayerProvider;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface IWritePlayerProvider extends IPlayerProvider {

    void add(ICloudPlayer player);

    void remove(ICloudPlayer player);
}