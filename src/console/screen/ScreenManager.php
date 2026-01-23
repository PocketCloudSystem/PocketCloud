<?php

namespace pocketcloud\cloud\console\screen;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\screen\impl\DefaultScreen;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\trait\SingletonTrait;
use RuntimeException;

final class ScreenManager implements Tickable {
    use SingletonTrait;

    private ?IScreen $currentScreen = null;

    public function __construct() {
        self::setInstance($this);
    }

    public function tick(int $currentTick): void {
        $this->currentScreen?->tick($currentTick);
    }

    public function setCurrentScreen(?IScreen $currentScreen): void {
        $this->currentScreen?->onRemove(PocketCloud::getInstance()->getTick());
        $this->currentScreen = $currentScreen;
        $this->currentScreen->initialize(Console::getInstance());
    }

    public function resetScreen(): void {
        $this->setCurrentScreen(new DefaultScreen());
    }

    public function getCurrentScreen(): IScreen {
        return $this->currentScreen ?? throw new RuntimeException("Current screen cannot be null");
    }
}