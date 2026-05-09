package de.pocketcloud.cloud;

import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.CloudShutdownHook;
import de.pocketcloud.cloud.console.command.CommandManager;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.NettyServer;
import de.pocketcloud.cloud.tick.Ticker;
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

    private boolean running;
    private boolean hasStopped = false;

    private final Ticker ticker;

    private final CloudConsole console;
    private final CommandManager commandManager;
    private final NettyServer network;

    public PocketCloud() throws IOException {
        instance = this;
        running = true;

        Thread.setDefaultUncaughtExceptionHandler((_, throwable) -> CloudLogger.get().exception(throwable));

        CloudLogger.set(CloudLogger.tmp("storage/cloud.log"));

        console = new CloudConsole();
        console.install();
        console.start();

        commandManager = new CommandManager();
        network = new NettyServer();
        ticker = new Ticker(console::pollCommands);

        network.start();

        CloudLogger.get().info("PocketCloud started");
        Runtime.getRuntime().addShutdownHook(new CloudShutdownHook());

        ticker.tick();
    }

    public void shutdown() {
        if (!running || hasStopped) return;
        CloudLogger.get().info("Shutting down...");

        hasStopped = true;
        running = false;

        network.close();

        CloudLogger.get().success("Done");
        console.uninstall();
    }
}