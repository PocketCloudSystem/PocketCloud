<?php

namespace pocketcloud\event;

use pocketcloud\utils\SingletonTrait;

class EventManager {
    use SingletonTrait;

    private array $handlers = [];

    public function registerEvent(string $eventClass, \Closure $closure) {
        if (is_subclass_of($eventClass, Event::class)) {
            $this->handlers[$eventClass][] = $closure;
        }
    }

    public function registerListener(Listener $listener) {
        $reflection = new \ReflectionClass($listener);
        foreach ($reflection->getMethods() as $method) {
            if (!$method->isAbstract() && !$method->isStatic() && $method->isPublic() && $method->getNumberOfParameters() == 1) {
                $event = $method->getParameters()[0]->getType()->getName();
                if (is_subclass_of($event, Event::class)) $this->handlers[$event][] = $method->getClosure($listener);
            }
        }
    }

    public function callEvent(Event $event) {
        foreach (($this->handlers[$event::class] ?? []) as $handler) {
            ($handler)($event);
        }
    }
}