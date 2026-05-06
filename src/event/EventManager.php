<?php

namespace pocketcloud\cloud\event;

use Closure;
use pocketcloud\cloud\plugin\CloudPlugin;
use pocketcloud\cloud\util\trait\SingletonTrait;
use ReflectionClass;
use ReflectionException;

final class EventManager {
    use SingletonTrait;

    private array $handlers = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function register(string $eventClass, Closure $closure, CloudPlugin $plugin): void {
        if (is_subclass_of($eventClass, Event::class)) $this->handlers[$plugin->getDescription()->getFullName()][$eventClass][] = $closure;
    }

    public function registerListener(Listener $listener, CloudPlugin $plugin): void {
        $reflection = new ReflectionClass($listener);
        foreach ($reflection->getMethods() as $method) {
            if (!$method->isAbstract() && !$method->isStatic() && $method->isPublic() && $method->getNumberOfParameters() == 1) {
                $event = $method->getParameters()[0]->getType()->getName();
                if (is_subclass_of($event, Event::class)) try {
                    $this->handlers[$plugin->getDescription()->getFullName()][$event][] = $method->getClosure($listener);
                } catch (ReflectionException $e) {
                    $plugin->getLogger()->exception($e);
                }
            }
        }
    }

    public function removeHandlers(CloudPlugin $plugin): void {
        if (isset($this->handlers[$plugin->getDescription()->getFullName()])) unset($this->handlers[$plugin->getDescription()->getFullName()]);
    }

    public function removeAll(): void {
        $this->handlers = [];
    }

    /**
     * @param Event $event
     * @return void
     * @internal
     */
    public function call(Event $event): void {
        foreach ($this->handlers as $pluginHandler) {
            foreach (($pluginHandler[$event::class] ?? []) as $handler) {
                ($handler)($event);
            }
        }
    }
}