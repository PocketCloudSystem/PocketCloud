<?php

namespace pocketcloud\cloud\console\log\output;

final class OutputManager {

    private static ?OutputHandler $handler = null;

    public static function setHandler(OutputHandler $handler): void {
        self::$handler = $handler;
    }

    public static function getHandler(): OutputHandler {
        if (self::$handler === null) self::$handler = new DefaultOutputHandler();
        return self::$handler;
    }

    public static function reset(): void {
        self::$handler = new DefaultOutputHandler();
    }
}