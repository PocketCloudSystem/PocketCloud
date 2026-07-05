package de.pocketcloud.cloud.console.util;

import de.pocketcloud.common.util.NetUtils.DownloadProgress;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.utils.*;

import java.util.List;

public final class DownloadProgressBar {

    private static final int BAR_WIDTH = 30;

    private final String filename;
    private final Terminal terminal;
    private final LineReader lineReader;
    private final Display display;

    public DownloadProgressBar(String filename, Terminal terminal, LineReader lineReader) {
        this.filename = filename;
        this.terminal = terminal;
        this.lineReader = lineReader;
        this.display = new Display(terminal, false);
        this.display.resize(1, terminal.getWidth());
    }

    public void start() {
        terminal.puts(InfoCmp.Capability.cursor_invisible);
        terminal.flush();
        lineReader.callWidget(LineReader.CLEAR);
    }

    public void update(DownloadProgress p) {
        display.update(List.of(buildBar(p)), 0);
        terminal.flush();
    }

    public void finish() {
        terminal.puts(InfoCmp.Capability.cursor_normal);
        terminal.writer().println();
        terminal.flush();
        lineReader.callWidget(LineReader.REDRAW_LINE);
        lineReader.callWidget(LineReader.REDISPLAY);
        terminal.flush();
    }

    public void abort() {
        terminal.puts(InfoCmp.Capability.cursor_normal);
        display.clear();
        terminal.writer().println();
        terminal.flush();
        lineReader.callWidget(LineReader.REDRAW_LINE);
        lineReader.callWidget(LineReader.REDISPLAY);
        terminal.flush();
    }

    private AttributedString buildBar(DownloadProgress p) {
        int filled = p.percent() >= 0 ? (int) (p.percent() / 100 * BAR_WIDTH) : 0;
        AttributedStringBuilder builder = new AttributedStringBuilder();

        if (p.percent() >= 100.0) {
            builder.append("Downloaded ")
                    .append(filename)
                    .append(" [")
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                    .append("█".repeat(BAR_WIDTH))
                    .style(AttributedStyle.DEFAULT)
                    .append("] 100.0%")
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                    .append(String.format("  %s  |  Ø %s", p.formatSize(p.totalBytes()), p.formatSpeed()));
        } else {
            String bar = "█".repeat(filled) + "░".repeat(BAR_WIDTH - filled);
            String sizeInfo = p.totalBytes() > 0
                    ? p.formatSize(p.downloadedBytes()) + " / " + p.formatSize(p.totalBytes())
                    : p.formatSize(p.downloadedBytes());

            builder.append("Downloading ")
                    .append(filename).append("...")
                    .append(" [")
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                    .append(bar)
                    .style(AttributedStyle.DEFAULT)
                    .append("] ")
                    .style(AttributedStyle.BOLD)
                    .append(String.format("%.1f%%", Math.max(p.percent(), 0)))
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                    .append(String.format("  %s  |  %s  |  Time left: %s", sizeInfo, p.formatSpeed(), p.formatEta()));
        }

        return builder.toAttributedString();
    }
}