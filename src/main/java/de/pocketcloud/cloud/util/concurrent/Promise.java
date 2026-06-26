package de.pocketcloud.cloud.util.concurrent;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Promise<T> {

    private final CompletableFuture<FutureResult<T>> future;
    private static Executor defaultExecutor = ForkJoinPool.commonPool(); // Can be changed

    public Promise() {
        this.future = new CompletableFuture<>();
    }

    private Promise(CompletableFuture<FutureResult<T>> future) {
        this.future = future;
    }

    public static <T> Promise<T> of(CompletableFuture<FutureResult<T>> future) {
        return new Promise<>(future);
    }

    public static <T> Promise<T> resolved(T value) {
        Promise<T> p = new Promise<>();
        p.resolve(value);
        return p;
    }

    public static <T> Promise<T> rejected(String error) {
        Promise<T> p = new Promise<>();
        p.reject(error);
        return p;
    }

    public static <T> Promise<T> failed(Throwable e) {
        Promise<T> p = new Promise<>();
        p.fail(e);
        return p;
    }

    public static <T> Promise<T> supplyAsync(Supplier<T> supplier) {
        return supplyAsync(supplier, defaultExecutor);
    }

    @SuppressWarnings("unchecked")
    public static <T> Promise<T> supplyAsync(Supplier<T> supplier, Executor executor) {
        Promise<T> promise = new Promise<>();

        CompletableFuture.supplyAsync(supplier, executor)
                .thenAccept(result -> {
                    if (result instanceof FutureResult) {
                        FutureResult<T> fr = (FutureResult<T>) result;
                        if (fr.success()) {
                            promise.resolve(fr.value());
                        } else {
                            promise.reject(fr.error(), fr.value());
                        }
                    } else {
                        promise.resolve(result);
                    }
                })
                .exceptionally(ex -> {
                    promise.fail(ex);
                    return null;
                });

        return promise;
    }

    public static Promise<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, defaultExecutor);
    }

    public static Promise<Void> runAsync(Runnable runnable, Executor executor) {
        Promise<Void> promise = new Promise<>();
        CompletableFuture.runAsync(runnable, executor)
                .thenRun(() -> promise.resolve(null))
                .exceptionally(ex -> {
                    promise.fail(ex);
                    return null;
                });
        return promise;
    }

    public static void setDefaultExecutor(Executor executor) {
        defaultExecutor = executor != null ? executor : ForkJoinPool.commonPool();
    }

    public boolean resolve(T value) {
        return future.complete(new FutureResult<>(true, value, null));
    }

    public boolean reject(String error) {
        return future.complete(new FutureResult<>(false, null, error));
    }

    public boolean reject(String error, T value) {
        return future.complete(new FutureResult<>(false, value, error));
    }

    public boolean fail(Throwable throwable) {
        return future.completeExceptionally(throwable);
    }

    public Promise<T> thenSuccess(Consumer<T> consumer) {
        future.thenAccept(result -> {
            if (result.success() && consumer != null) {
                consumer.accept(result.value());
            }
        });
        return this;
    }

    public Promise<T> thenReject(Consumer<String> consumer) {
        future.thenAccept(result -> {
            if (!result.success() && consumer != null) {
                consumer.accept(result.error());
            }
        });
        return this;
    }

    public Promise<T> failure(Consumer<Throwable> failure) {
        future.exceptionally(ex -> {
            if (failure != null) failure.accept(ex);
            return null;
        });
        return this;
    }

    public <U> Promise<U> thenApply(Function<T, U> mapper) {
        return Promise.of(
                future.thenApply(result -> {
                    if (result.success()) {
                        return new FutureResult<>(true, mapper.apply(result.value()), null);
                    }
                    return new FutureResult<>(false, null, result.error());
                })
        );
    }

    public <U> Promise<U> thenCompose(Function<T, Promise<U>> mapper) {
        CompletableFuture<FutureResult<U>> composed = future.thenCompose(result -> {
            if (result.success()) {
                return mapper.apply(result.value()).future;
            } else {
                return CompletableFuture.completedFuture(new FutureResult<>(false, null, result.error()));
            }
        });
        return Promise.of(composed);
    }

    public T join() {
        FutureResult<T> result = future.join();
        if (result.success()) return result.value();
        throw new PromiseException(result.error());
    }

    public T get() throws ExecutionException, InterruptedException {
        FutureResult<T> result = future.get();
        if (result.success()) return result.value();
        throw new ExecutionException(result.error(), null);
    }

    public boolean isDone() {
        return future.isDone();
    }

    public boolean isCompletedExceptionally() {
        return future.isCompletedExceptionally();
    }

    public CompletableFuture<FutureResult<T>> toCompletableFuture() {
        return future;
    }

    public static Promise<Void> all(Promise<?>... promises) {
        if (promises == null || promises.length == 0) {
            return Promise.resolved(null);
        }

        CompletableFuture<?>[] futures = new CompletableFuture[promises.length];

        for (int i = 0; i < promises.length; i++) {
            futures[i] = promises[i].toCompletableFuture();
        }

        return Promise.of(CompletableFuture.allOf(futures).thenApply(_ -> new FutureResult<>(true, null, null)));
    }

    public static Promise<Object> anyOf(Promise<?>... promises) {
        if (promises == null || promises.length == 0) {
            return Promise.rejected("No promises provided");
        }

        CompletableFuture<?>[] futures = new CompletableFuture[promises.length];

        for (int i = 0; i < promises.length; i++) {
            futures[i] = promises[i].toCompletableFuture();
        }

        return Promise.of(CompletableFuture.anyOf(futures).thenApply(result -> new FutureResult<>(true, result, null)));
    }

    private static class PromiseException extends RuntimeException {
        public PromiseException(String message) {
            super(message);
        }
    }
}