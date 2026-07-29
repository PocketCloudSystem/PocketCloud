package de.pocketcloud.cloud.setup;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class QuestionBuilder<T> {

    private final String id;
    private final String question;
    private boolean canSkipped = false;
    private List<String> possibleAnswers = List.of();
    private String defaultValueMessage;
    private T defaultValue;
    private String recommendation;
    private final QuestionParser<T> parser;
    private Consumer<T> resultHandler;

    private QuestionBuilder(String id, String question, QuestionParser<T> parser) {
        this.id = id;
        this.question = question;
        this.parser = parser;
    }

    public static QuestionBuilder<String> builder(String id, String question) {
        return new QuestionBuilder<>(id, question, defaultParser());
    }

    public QuestionBuilder<T> canSkipped(boolean value) {
        this.canSkipped = value;
        return this;
    }

    public QuestionBuilder<T> possibleAnswers(String... answers) {
        this.possibleAnswers = Arrays.asList(answers);
        return this;
    }

    public QuestionBuilder<T> possibleAnswers(List<String> answers) {
        this.possibleAnswers = answers;
        return this;
    }

    public QuestionBuilder<T> defaultValue(String displayDefault, T value) {
        this.defaultValueMessage = displayDefault;
        this.defaultValue = value;
        return this;
    }

    public QuestionBuilder<T> recommendation(String recommendation) {
        this.recommendation = recommendation;
        return this;
    }

    public <R> QuestionBuilder<R> parser(QuestionParser<R> parser) {
        QuestionBuilder<R> next = new QuestionBuilder<>(this.id, this.question, parser);
        next.canSkipped = this.canSkipped;
        next.possibleAnswers = this.possibleAnswers;
        next.recommendation = this.recommendation;
        return next;
    }

    public QuestionBuilder<T> resultHandler(Consumer<T> handler) {
        this.resultHandler = handler;
        return this;
    }

    public Question<T> build() {
        return new Question<>(
                id,
                question,
                canSkipped,
                possibleAnswers,
                defaultValueMessage,
                defaultValue,
                recommendation,
                parser,
                resultHandler
        );
    }

    public static QuestionParser<String> defaultParser() {
        return (input, error) -> input;
    }
}