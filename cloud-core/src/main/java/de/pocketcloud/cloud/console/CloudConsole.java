package de.pocketcloud.cloud.console;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.util.InterruptionResult;
import de.pocketcloud.common.lifecycle.Tickable;
import lombok.Getter;
import lombok.Setter;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.InfoCmp;
import org.jline.utils.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

@Getter
public final class CloudConsole extends Thread implements Tickable {

    private final static String DEFAULT_PROMPT = "§c" + System.getProperty("user.name").toLowerCase() + "§8@§bcloud §8» §r";

    private final BlockingQueue<String> consoleQueue = new LinkedBlockingQueue<>();
    @Setter
    private Supplier<InterruptionResult> interruptionHandler = () -> PocketCloud.instance().screens().get().onCancel(PocketCloud.instance().currentTick());

    private final Object readerLock = new Object();
    private Terminal terminal;
    private LineReader reader;
    private Status status;
    private String prompt = DEFAULT_PROMPT;

    public CloudConsole install() throws IOException {
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
        status = Status.getStatus(terminal);

        setPromptInternal(prompt);

        return this;
    }

    public void uninstall() {
        this.interrupt();

        synchronized (readerLock) {
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
    }

    public void clear() {
        synchronized (readerLock) {
            try {
                if (terminal != null) {
                    terminal.puts(InfoCmp.Capability.clear_screen);
                    terminal.puts(InfoCmp.Capability.cursor_home);
                    terminal.flush();

                    reader.callWidget(LineReader.REDRAW_LINE);
                }
            } catch (Exception _) {}
        }
    }

    @Override
    public void run() {
        while (PocketCloud.instance().running()) {
            try {
                String line = reader.readLine(ConsoleColor.convert(prompt, false));
                if (line == null) continue;
                consoleQueue.offer(line);
            } catch (UserInterruptException _) {
                InterruptionResult res = interruptionHandler.get();
                if (res == InterruptionResult.INTERRUPT) break;
            } catch (EndOfFileException _) {
                PocketCloud.instance().screens().reset();
                consoleQueue.offer("exit -y");
                break;
            }
        }
    }

    public void print(String line) {
        if (reader == null) {
            System.out.println(line);
            return;
        }

        synchronized (readerLock) {
            reader.printAbove(AttributedString.fromAnsi(ConsoleColor.convert(line)));
        }
    }

    @Override
    public void tick(long currentTick) {
        pollCommands();
    }

    public void pollCommands() {
        String line;
        if (!PocketCloud.instance().running()) return;
        while ((line = consoleQueue.poll()) != null) {
            PocketCloud.instance().screens().get().handleInput(line);
        }
    }

    public void enableHistory(boolean enabled) {
        synchronized (readerLock) {
            if (reader != null) {
                reader.setVariable(LineReader.DISABLE_HISTORY, !enabled);
            }
        }
    }

    public void showCursor(boolean enabled) {
        synchronized (readerLock) {
            if (terminal != null) {
                terminal.puts(InfoCmp.Capability.cursor_invisible, enabled ? 0 : 1);
                terminal.flush();
            }
        }
    }

    public void showTyping(boolean enabled) {
        synchronized (readerLock) {
            if (terminal != null) {
                terminal.echo(enabled);
            }
        }
    }

    public void enableCompletion(boolean enabled) {
        synchronized (readerLock) {
            if (reader != null) {
                reader.setVariable(LineReader.DISABLE_COMPLETION, !enabled);
            }
        }
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

    public void showStatus(String... lines) {
        if (terminal == null) return;
        synchronized (readerLock) {
            List<AttributedString> rendered = Arrays.stream(lines)
                    .map(ConsoleColor::convert)
                    .map(AttributedString::fromAnsi)
                    .toList();
            status.update(rendered);
        }
    }

    public void hideStatus() {
        if (terminal == null || reader == null) return;
        synchronized (readerLock) {
            if (status != null) status.update(Collections.emptyList());
        }
    }

    public void setInput(String input) {
        if (reader == null) return;
        synchronized (readerLock) {
            try {
                reader.getBuffer().clear();
                reader.getBuffer().write(input);
                reader.callWidget(LineReader.REDRAW_LINE);
                reader.callWidget(LineReader.REDISPLAY);
            } catch (Exception _) {}
        }
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
        setPromptInternal(prompt);
    }

    private void setPromptInternal(String prompt) {
        if (reader == null) return;
        synchronized (readerLock) {
            ((LineReaderImpl) reader).setPrompt(ConsoleColor.convert(prompt, false));
            try {
                terminal.puts(InfoCmp.Capability.carriage_return);
                terminal.puts(InfoCmp.Capability.clr_eol);
                terminal.flush();
                reader.callWidget(LineReader.REDRAW_LINE);
                reader.callWidget(LineReader.REDISPLAY);
            } catch (Exception ignored) {}
        }
    }

    public void resetPrompt() {
        setPrompt(DEFAULT_PROMPT);
    }

    public void resetInterruptionHandler() {
        interruptionHandler = () -> PocketCloud.instance().screens().get().onCancel(PocketCloud.instance().currentTick());
    }
}