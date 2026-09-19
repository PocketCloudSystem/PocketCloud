package de.pocketcloud.cloud.console;

import de.pocketcloud.cloud.PocketCloud;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public final class ConsoleTabCompleter implements Completer {

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
        PocketCloud.instance().screens().get().onTabComplete(lineReader, parsedLine, list);
    }
}