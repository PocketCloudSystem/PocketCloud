<?php

namespace pocketcloud\cloud;

use pocketcloud\cloud\loader\ClassLoader;
use pocketcloud\cloud\util\SingletonTrait;
use pocketmine\snooze\SleeperHandler;

final class PocketCloud {
    use SingletonTrait;

    private SleeperHandler $sleeperHandler;

    public function __construct(
        private readonly ClassLoader $classLoader
    ) {
        $this->sleeperHandler = new SleeperHandler();
    }

    public function getSleeperHandler(): SleeperHandler {
        return $this->sleeperHandler;
    }

    public function getClassLoader(): ClassLoader {
        return $this->classLoader;
    }

    public static function getInstance(): ?self {
        return self::$instance;
    }
}