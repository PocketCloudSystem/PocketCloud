<?php

namespace pocketcloud\util;

trait SingletonTrait {

    /** @ignored */
    private static ?self $instance = null;

    public static function getInstance(): self {
        if (self::$instance === null) self::$instance = new self;
        return self::$instance;
    }

    public static function setInstance(?self $instance): void {
        self::$instance = $instance;
    }
}