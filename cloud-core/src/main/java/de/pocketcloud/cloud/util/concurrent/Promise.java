package de.pocketcloud.cloud.util.concurrent;

import de.pocketcloud.cloud.console.log.CloudLogger;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Promise<T> {

    private final CompletableFuture<T> future;

    private static volatile Executor defaultExecutor = ForkJoinPool.commonPool();

    public Promise() {
        this.future = new CompletableFuture<>();
    }

    private Promise(CompletableFuture<T> future) {
        this.future = future;
    }

    public static <T> Promise<T> of(CompletableFuture<T> future) {
        return new Promise<>(future);
    }

    public static <T> Promise<T> resolved(T value) {
        return of(CompletableFuture.completedFuture(value));
    }

    public static <T> Promise<T> rejected(Throwable e) {
        return of(CompletableFuture.failedFuture(e));
    }

    public static <T> Promise<T> supplyAsync(Supplier<T> supplier) {
        return supplyAsync(supplier, defaultExecutor);
    }

    public static <T> Promise<T> supplyAsync(Supplier<T> supplier, Executor executor) {
        return of(CompletableFuture.supplyAsync(supplier, executor));
    }

    public static <T> Promise<T> supplyAsyncResult(Supplier<PromiseResult<T>> supplier) {
        return supplyAsyncResult(supplier, defaultExecutor);
    }

    public static <T> Promise<T> supplyAsyncResult(Supplier<PromiseResult<T>> supplier, Executor executor) {
        Promise<T> promise = new Promise<>();
        CompletableFuture.supplyAsync(supplier, executor)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        promise.reject(unwrap(ex));
                    } else if (result.success()) {
                        promise.resolve(result.value());
                    } else {
                        promise.reject(result.error());
                    }
                });
        return promise;
    }

    public static Promise<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, defaultExecutor);
    }

    public static Promise<Void> runAsync(Runnable runnable, Executor executor) {
        return of(CompletableFuture.runAsync(runnable, executor));
    }

    public static void setDefaultExecutor(Executor executor) {
        defaultExecutor = executor != null ? executor : ForkJoinPool.commonPool();
    }

    public boolean resolve(T value) {
        return future.complete(value);
    }

    public boolean reject(Throwable error) {
        return future.completeExceptionally(error != null ? error : new RuntimeException("rejected with null error"));
    }

    public Promise<T> thenSuccess(Consumer<T> consumer) {
        future.thenAccept(value -> {
            if (consumer == null) return;
            try {
                consumer.accept(value);
            } catch (Throwable t) {
                CloudLogger.get().exception("Unhandled exception in Promise#thenSuccess callback", t);
            }
        });
        return this;
    }

    public Promise<T> failure(Consumer<Throwable> onFailure) {
        future.exceptionally(ex -> {
            if (onFailure == null) return null;
            try {
                onFailure.accept(unwrap(ex));
            } catch (Throwable t) {
                CloudLogger.get().exception("Unhandled exception in Promise#failure callback", t);
            }
            return null;
        });
        return this;
    }

    public <U> Promise<U> thenApply(Function<T, U> mapper) {
        return of(future.thenApply(mapper));
    }

    public <U> Promise<U> thenCompose(Function<T, Promise<U>> mapper) {
        return of(future.thenCompose(value -> {
            Promise<U> next = mapper.apply(value);
            if (next == null) return CompletableFuture.failedFuture(new NullPointerException("mapper returned null Promise"));
            return next.future;
        }));
    }

    public T join() throws Throwable {
        try {
            return future.join();
        } catch (CompletionException e) {
            throw unwrap(e);
        }
    }

    public T get() throws Throwable {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw unwrap(e);
        }
    }

    public boolean isDone() {
        return future.isDone();
    }

    public boolean isCompletedExceptionally() {
        return future.isCompletedExceptionally();
    }

    public CompletableFuture<T> toCompletableFuture() {
        return future;
    }

    public static Promise<Void> all(Promise<?>... promises) {
        if (promises == null || promises.length == 0) return Promise.resolved(null);

        CompletableFuture<?>[] futures = new CompletableFuture[promises.length];
        for (int i = 0; i < promises.length; i++) {
            futures[i] = promises[i].future;
        }

        return of(CompletableFuture.allOf(futures));
    }

    @SuppressWarnings("rawtypes")
    public static Promise<Object> anyOf(Promise<?>... promises) {
        if (promises == null || promises.length == 0) {
            return Promise.rejected(new IllegalArgumentException("No promises provided"));
        }

        CompletableFuture[] futures = new CompletableFuture[promises.length];
        for (int i = 0; i < promises.length; i++) {
            futures[i] = promises[i].future;
        }

        return of(CompletableFuture.anyOf(futures));
    }

    private static Throwable unwrap(Throwable t) {
        if ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
            return t.getCause();
        }
        return t;
    }
}