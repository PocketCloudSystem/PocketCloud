package de.pocketcloud.cloud;

import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.command.CommandManager;
import de.pocketcloud.cloud.console.log.CloudLogger;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;

@Getter
@Accessors(fluent = true)
public final class PocketCloud {

    public static void main(String[] args) throws IOException {
        new PocketCloud();
    }

    @Getter
    @Accessors(fluent = false)
    private static PocketCloud instance = null;

    private boolean running = false;

    private final CloudConsole console;
    private final CommandManager commandManager;

    public PocketCloud() throws IOException {
        instance = this;
        running = true;

        console = new CloudConsole();
        console.install();
        console.start();

        CloudLogger.get().info("PocketCloud started");

        commandManager = new CommandManager();

        Runtime.getRuntime().addShutdownHook(new Thread(console::uninstall));
    }
}