package de.pocketcloud.cloud.player.executor;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.executor.IPlayerExecutor;

import java.util.UUID;

public final class CloudPlayerExecutor implements IPlayerExecutor {

    //TODO Packets etc

    @Override
    public void sendMessage(UUID uuid, String message) {

    }

    @Override
    public void sendPopup(UUID uuid, String popup, String subtitle) {

    }

    @Override
    public void sendTip(UUID uuid, String tip) {

    }

    @Override
    public void sendTitle(UUID uuid, String title, String subtitle, int fadeIn, int stay, int fadeOut) {

    }

    @Override
    public void sendActionbarMessage(UUID uuid, String message, int fadeIn, int stay, int fadeOut) {

    }

    @Override
    public void sendToast(UUID uuid, String title, String body) {

    }

    @Override
    public void kick(UUID uuid, String reason, String disconnectScreenMessage) {

    }

    @Override
    public void transfer(UUID uuid, ICloudServer server) {

    }

    @Override
    public void sendMessage(String nameOrXuid, String message) {

    }

    @Override
    public void sendPopup(String nameOrXuid, String popup, String subtitle) {

    }

    @Override
    public void sendTip(String nameOrXuid, String tip) {

    }

    @Override
    public void sendTitle(String nameOrXuid, String title, String subtitle, int fadeIn, int stay, int fadeOut) {

    }

    @Override
    public void sendActionbarMessage(String nameOrXuid, String message, int fadeIn, int stay, int fadeOut) {

    }

    @Override
    public void sendToast(String nameOrXuid, String title, String body) {

    }

    @Override
    public void kick(String nameOrXuid, String reason, String disconnectScreenMessage) {

    }

    @Override
    public void transfer(String nameOrXuid, ICloudServer server) {

    }
}