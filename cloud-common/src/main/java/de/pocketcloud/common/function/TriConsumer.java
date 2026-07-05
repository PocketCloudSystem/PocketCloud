package de.pocketcloud.common.function;

@FunctionalInterface
public interface TriConsumer<A, B, C> {

    void accept(A a, B b, C c);
}