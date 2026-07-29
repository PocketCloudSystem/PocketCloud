package de.pocketcloud.cloud.setup;

@FunctionalInterface
public interface QuestionParser<T> {

    T parse(String input, ErrorHolder error);
}