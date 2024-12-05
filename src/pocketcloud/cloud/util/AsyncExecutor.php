<?php

namespace pocketcloud\cloud\util;

use Closure;
use pocketcloud\cloud\scheduler\AsyncClosureTask;
use pocketcloud\cloud\scheduler\AsyncPool;
use pocketcloud\cloud\scheduler\AsyncTask;

final class AsyncExecutor {

    public static function execute(Closure $asyncClosure, ?Closure $syncClosure = null, mixed ...$args): void {
        AsyncPool::getInstance()->submitTask(new AsyncClosureTask(fn(AsyncTask $task) => ($asyncClosure)(), function(mixed $result) use($syncClosure, $args): void {
            if ($syncClosure !== null) $syncClosure($result, ...$args);
        }));
    }
}