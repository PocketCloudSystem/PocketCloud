<?php

namespace pocketcloud\cloud\console\screen;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\screen\impl\DefaultScreen;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\trait\SingletonTrait;
use RuntimeException;

final class ScreenManager implements Tickable {
    use SingletonTrait;

    private ?Screen $currentScreen = null;

    public function __construct() {
        self::setInstance($this);
    }

    public function tick(int $currentTick): void {
        $this->currentScreen?->tick($currentTick);
    }

    public function setCurrentScreen(Screen $currentScreen): void {
        $this->currentScreen?->onRemove($Server::getInstance()->tick);
        $this->currentScreen = $currentScreen;
        $this->currentScreen->initialize(Console::getInstance());
    }

    public function resetScreen(): void {
        $this->setCurrentScreen(new DefaultScreen());
    }

    public function getCurrentScreen(): Screen {
        return $this->currentScreen ?? throw new RuntimeException("Current screen cannot be null");
    }
}