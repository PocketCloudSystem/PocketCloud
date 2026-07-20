package de.pocketcloud.bridge.command.sender;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.ConsoleCommandSender;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WDPECloudCommandSender extends ConsoleCommandSender {

    private final String id;
    @Getter
    private final List<String> cachedMessages = new ArrayList<>();

    public WDPECloudCommandSender(String id) {
        super(ProxyServer.getInstance());
        this.id = id;
    }

    @Override
    public void sendMessage(String message) {
        super.sendMessage(message);
        cachedMessages.addAll(Arrays.asList(message.split("\n")));
    }

    @Override
    @NotNull
    public String getName() {
        return "pocketcloud-" + this.id;
    }
}