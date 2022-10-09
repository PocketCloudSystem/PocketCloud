<?php

namespace pocketcloud\utils;

trait SingletonTrait {

    private static ?self $instance = null;

    public function __construct() {
        self::setInstance($this);
    }

    public static function getInstance(): self {
        if (self::$instance === null) self::$instance = new self;
        return self::$instance;
    }

    public static function setInstance(?self $instance): void {
        self::$instance = $instance;
    }
}