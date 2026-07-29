package de.pocketcloud.cloud.setup;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Getter
public final class Question<T> {

    private final String id;
    private final String question;
    private final boolean canSkipped;
    private final List<String> possibleAnswers;
    private final String defaultValueMessage;
    private final T defaultValue;
    private final String recommendation;
    private final QuestionParser<T> parser;
    private final Consumer<T> resultHandler;

    Question(
            String id,
            String question,
            boolean canSkipped,
            List<String> possibleAnswers,
            String defaultValueMessage,
            T defaultValue,
            String recommendation,
            QuestionParser<T> parser,
            Consumer<T> resultHandler
    ) {
        this.id = id;
        this.question = question;
        this.canSkipped = canSkipped;
        this.possibleAnswers = possibleAnswers == null ? Collections.emptyList() : possibleAnswers;
        this.defaultValueMessage = defaultValueMessage;
        this.defaultValue = defaultValue;
        this.recommendation = recommendation;
        this.parser = parser;
        this.resultHandler = resultHandler;
    }
}