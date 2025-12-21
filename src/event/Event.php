<?php

namespace pocketcloud\cloud\event;

use ReflectionClass;

abstract class Event {

    public function getName(): string {
        return new ReflectionClass($this)->getShortName();
    }

    public function call(): void {
        EventManager::getInstance()->call($this);
    }
}