package de.pocketcloud.cloud.console;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.common.lifecycle.Tickable;
import lombok.Getter;
import lombok.Setter;
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

    @Setter
    private String prompt = "";

    private final BlockingQueue<String> consoleQueue = new LinkedBlockingQueue<>();
    private Terminal terminal;
    private LineReader reader;

    public CloudConsole install() throws IOException {
        resetPrompt();
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

        terminal.handle(Terminal.Signal.INT, _ -> PocketCloud.instance().screens().get().onCancel(PocketCloud.instance().currentTick()));
        return this;
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
            terminal.puts(InfoCmp.Capability.clear_screen);
            terminal.flush();
        } catch (Exception _) {}
    }

    @Override
    public void run() {
        while (PocketCloud.instance().running()) {
            try {
                String line = reader.readLine(ConsoleColor.convert(prompt));
                if (line == null) continue;
                consoleQueue.offer(line);
            } catch (UserInterruptException | EndOfFileException _) {
                consoleQueue.offer("exit -y");
                break;
            }
        }
    }

    public void print(String line) {
        CloudConsole console = PocketCloud.instance().console();
        if (console != null) {
            LineReader reader = console.getReader();
            if (reader != null) {
                reader.printAbove(line);
            } else System.out.println(line);
        } else System.out.println(line);
    }

    @Override
    public void tick(long currentTick) {
        pollCommands();
    }

    public void pollCommands() {
        String line;
        while ((line = consoleQueue.poll()) != null) {
            PocketCloud.instance().screens().get().handleInput(line);
        }
    }

    public void enableHistory(boolean enabled) {
        reader.setVariable(LineReader.DISABLE_HISTORY, !enabled);
    }

    public void showCursor(boolean enabled) {
        terminal.puts(InfoCmp.Capability.cursor_invisible, enabled);
    }

    public void showTyping(boolean enabled) {
        terminal.echo(enabled);
    }

    public void enableCompletion(boolean enabled) {
        reader.setVariable(LineReader.DISABLE_COMPLETION, !enabled);
    }

    public void enableHistory() {
        enableHistory(true);
    }

    public void disableHistory() {
        enableHistory(false);
    }

    public void showCursor() {
        showCursor(true);
    }

    public void hideCursor() {
        showCursor(false);
    }

    public void showTyping() {
        showTyping(true);
    }

    public void hideTyping() {
        showTyping(false);
    }

    public void enableCompletion() {
        enableCompletion(true);
    }

    public void disableCompletion() {
        enableCompletion(false);
    }

    public void resetPrompt() {
        this.prompt = "§c" + System.getProperty("user.name").toLowerCase() + "§8@§bcloud §8» §r";
    }
}