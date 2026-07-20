package de.pocketcloud.bridge.command.sender;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.powernukkitx.command.ConsoleCommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PNXCloudCommandSender extends ConsoleCommandSender {

    private final String id;
    @Getter
    private final List<String> cachedMessages = new ArrayList<>();

    public PNXCloudCommandSender(String id) {
        super();
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