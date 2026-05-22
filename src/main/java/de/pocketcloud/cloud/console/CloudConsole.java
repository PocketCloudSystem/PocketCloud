package de.pocketcloud.cloud.console;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.sender.ConsoleCommandSender;
import de.pocketcloud.cloud.tick.Tickable;
import lombok.Getter;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Getter
public final class CloudConsole extends Thread implements Tickable {

    private String prompt = "§c" + System.getProperty("user.name").toLowerCase() + "§8@§bcloud §8» §r";

    private final BlockingQueue<String> consoleQueue = new LinkedBlockingQueue<>();
    private Terminal terminal;
    private LineReader reader;

    public void install() throws IOException {
        terminal = TerminalBuilder.builder()
                .color(true)
                .encoding(StandardCharsets.UTF_8)
                .system(true)
                .build();

        reader = LineReaderBuilder.builder()
                .appName("PocketCloud")
                .option(LineReader.Option.HISTORY_BEEP, false)
                .option(LineReader.Option.HISTORY_IGNORE_DUPS, true)
                .option(LineReader.Option.HISTORY_IGNORE_SPACE, true)
                .completer(new ConsoleTabCompleter())
                .terminal(terminal)
                .build();

        terminal.flush();
    }

    public void uninstall() {
        this.interrupt();

        try {
            if (reader != null) {
                reader.getBuffer().clear();
                reader.callWidget(LineReader.REDRAW_LINE);
                reader.callWidget(LineReader.REDISPLAY);
                reader.getTerminal().close();
            }
        } catch (Exception _) {}

        try {
            if (terminal != null) {
                terminal.puts(InfoCmp.Capability.carriage_return);
                terminal.puts(InfoCmp.Capability.clr_eol);
                terminal.flush();
                terminal.close();
            }
        } catch (Exception _) {}
    }

    public void clear() {
        try {
            terminal.puts(org.jline.utils.InfoCmp.Capability.clear_screen);
            terminal.flush();
        } catch (Exception _) {}
    }

    @Override
    public void run() {
        while (PocketCloud.getInstance().running()) {
            try {
                String line = reader.readLine(ConsoleColor.convert(prompt));
                if (line == null || line.isBlank()) continue;
                consoleQueue.offer(line);
            } catch (UserInterruptException | EndOfFileException _) {
                consoleQueue.offer("exit -y");
                break;
            }
        }
    }

    @Override
    public void tick(long currentTick) {
        pollCommands();
    }

    public void pollCommands() {
        String line;
        while ((line = consoleQueue.poll()) != null) {
            PocketCloud.getInstance().commandManager().call(new ConsoleCommandSender(), line.split(" "));
        }
    }

    public CloudConsole setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }
}