<?php

namespace pocketcloud\cloud\event;

use pocketcloud\cloud\util\benchmark\Benchmark;
use ReflectionClass;
use RuntimeException;

abstract class Event {

    private const int MAX_EVENT_CALL_DEPTH = 50;
    private static int $eventCallDepth = 1;

    public function getName(): string {
        return new ReflectionClass($this)->getShortName();
    }

    public function call(): void {
        if (self::$eventCallDepth >= self::MAX_EVENT_CALL_DEPTH){
            throw new RuntimeException("Recursive event call detected (reached max depth of " . self::MAX_EVENT_CALL_DEPTH . " calls)");
        }

        Benchmark::startTiming($i = "event_" . strtolower($this->getName()) . "_call");

        ++self::$eventCallDepth;
        try {
            EventManager::getInstance()->call($this);
        } finally {
            --self::$eventCallDepth;
            Benchmark::stopTiming($i);
        }
    }
}