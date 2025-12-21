<?php

namespace pocketcloud\cloud\util\trait;

trait SingletonTrait {

    /** @ignored */
    protected static mixed $instance = null;

    public static function setInstance(mixed $instance): void {
        self::$instance = $instance;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self();
    }

    public static function isInitialized(): bool {
        return self::$instance !== null;
    }
}