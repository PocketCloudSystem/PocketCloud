<?php

namespace pocketcloud\util;

use pocketcloud\scheduler\AsyncClosureTask;
use pocketcloud\scheduler\AsyncPool;
use pocketcloud\scheduler\AsyncTask;

class AsyncExecutor {

    public static function execute(\Closure $asyncClosure, ?\Closure $syncClosure = null, mixed ...$args): void {
        AsyncPool::getInstance()->submitTask(new AsyncClosureTask(fn(AsyncTask $task) => ($asyncClosure)(), function(mixed $result) use($syncClosure, $args): void {
            if ($syncClosure !== null) $syncClosure($result, ...$args);
        }));
    }
}