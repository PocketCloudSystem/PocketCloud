<?php

namespace pocketcloud\util;

trait SingletonTrait {

    /** @ignored */
    private static ?self $instance = null;

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }

    public static function setInstance(?self $instance): void {
        self::$instance = $instance;
    }
}