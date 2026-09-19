package de.pocketcloud.cloud.console.screen.impl;

import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.screen.Screen;
import de.pocketcloud.cloud.console.util.InterruptionResult;
import de.pocketcloud.cloud.setup.Question;
import de.pocketcloud.cloud.setup.Setup;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.ArrayList;
import java.util.List;

public final class SetupScreen extends Screen {

    private final Setup setup;

    public SetupScreen(Setup setup) {
        this.setup = setup;
    }

    @Override
    public void initialize(CloudConsole console) {
        clear();
        console.disableHistory();
        enableCompletion();
    }

    @Override
    public void handleInput(String input) {
        setup.handleInput(input);
    }

    @Override
    public void tick(long currentTick) {}

    @Override
    public void onRemove(long currentTick) {
        clear();
        restoreAll();
        printLogCache();
    }

    @Override
    public InterruptionResult onCancel(long currentTick) {
        setup.cancel();
        return InterruptionResult.CONTINUE;
    }

    @Override
    public void onTabComplete(LineReader reader, ParsedLine parsedLine, List<Candidate> list) {
        Question<?> question = setup.getCurrentQuestion();
        if (question != null) {
            final List<String> words = new ArrayList<>(parsedLine.words());
            if (!question.getPossibleAnswers().isEmpty()) {
                if (words.isEmpty() || words.size() == 1) {
                    String current = words.isEmpty() ? "" : words.getFirst();
                    for (String possibleAnswer : question.getPossibleAnswers()) {
                        if (possibleAnswer.toLowerCase().startsWith(current)) {
                            list.add(new Candidate(possibleAnswer));
                        }
                    }
                }
            }
        }
    }
}