package de.pocketcloud.cloud;

import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.CloudShutdownHook;
import de.pocketcloud.cloud.console.command.CommandManager;
import de.pocketcloud.cloud.console.log.CloudLogger;
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

    private boolean running = false;
    private boolean hasStopped = false;

    private final Ticker ticker;

    private final CloudConsole console;
    private final CommandManager commandManager;

    public PocketCloud() throws IOException {
        instance = this;
        running = true;

        Thread.setDefaultUncaughtExceptionHandler((_, throwable) -> CloudLogger.get().exception(throwable));

        CloudLogger.set(CloudLogger.tmp("storage/cloud.log"));

        console = new CloudConsole();
        console.install();
        console.start();

        commandManager = new CommandManager();

        ticker = new Ticker(console::pollCommands);

        CloudLogger.get().info("PocketCloud started");
        Runtime.getRuntime().addShutdownHook(new CloudShutdownHook());

        ticker.tick();
    }

    public void shutdown() {
        if (!running || hasStopped) return;
        CloudLogger.get().info("Shutting down...");

        this.hasStopped = true;
        this.running = false;

        CloudLogger.get().success("Done");
        this.console.uninstall();
    }
}