<?php

namespace pocketcloud\event;

abstract class Event {

    public function getName(): string {
        return (new \ReflectionClass($this))->getShortName();
    }

    public function call(): void {
        EventManager::getInstance()->callEvent($this);
    }
}