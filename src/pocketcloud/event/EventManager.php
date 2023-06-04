<?php

namespace pocketcloud\event;

use pocketcloud\plugin\CloudPlugin;
use pocketcloud\util\SingletonTrait;

class EventManager {
    use SingletonTrait;

    private array $handlers = [];

    public function registerEvent(string $eventClass, \Closure $closure, CloudPlugin $plugin) {
        if (is_subclass_of($eventClass, Event::class)) {
            $this->handlers[$plugin->getDescription()->getFullName()][$eventClass][] = $closure;
        }
    }

    public function registerListener(Listener $listener, CloudPlugin $plugin) {
        $reflection = new \ReflectionClass($listener);
        foreach ($reflection->getMethods() as $method) {
            if (!$method->isAbstract() && !$method->isStatic() && $method->isPublic() && $method->getNumberOfParameters() == 1) {
                $event = $method->getParameters()[0]->getType()->getName();
                if (is_subclass_of($event, Event::class)) $this->handlers[$plugin->getDescription()->getFullName()][$event][] = $method->getClosure($listener);
            }
        }
    }

    public function removeHandlers(CloudPlugin $plugin) {
        if (isset($this->handlers[$plugin->getDescription()->getFullName()])) {
            unset($this->handlers[$plugin->getDescription()->getFullName()]);
        }
    }

    public function removeAll() {
        $this->handlers = [];
    }

    public function callEvent(Event $event) {
        foreach ($this->handlers as $pluginHandler) {
            foreach (($pluginHandler[$event::class] ?? []) as $handler) {
                ($handler)($event);
            }
        }
    }
}