package de.pocketcloud.cloud.console;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.sender.ConsoleCommandSender;
import lombok.Getter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Getter
public final class CloudConsole extends Thread {

    private String prompt = "§c" + System.getProperty("user.name").toLowerCase() + "§8@§bcloud §8» §r";

    private Terminal terminal;
    private LineReader reader;

    public void install() throws IOException {
        terminal = TerminalBuilder.builder().color(true).encoding(StandardCharsets.UTF_8).system(true).build();
        reader = LineReaderBuilder.builder()
                .appName("PocketCloud")
                .option(LineReader.Option.HISTORY_BEEP, false)
                .option(LineReader.Option.HISTORY_IGNORE_DUPS, true)
                .option(LineReader.Option.HISTORY_IGNORE_SPACE, true)
                .completer(new ConsoleTabCompleter())
                .terminal(terminal)
                .build();

        //TODO logger
    }

    public void uninstall() {
        try {
            if (reader != null) {
                reader.getTerminal().close();
            }
        } catch (Exception _) {}

        try {
            if (terminal != null) {
                terminal.flush();
                terminal.close();
            }
        } catch (Exception _) {}

        this.interrupt();
    }

    @Override
    public void run() {
        while (PocketCloud.getInstance().running()) {
            try {
                String line = reader.readLine(ConsoleColor.convert(prompt));
                if (line == null || line.isBlank()) continue;

                PocketCloud.getInstance().commandManager().call(new ConsoleCommandSender(), line.split(" "));
            } catch (Exception _) {}
        }
    }

    public CloudConsole setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }
}