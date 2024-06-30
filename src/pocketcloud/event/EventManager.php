<?php

namespace pocketcloud\event;

use Closure;
use pocketcloud\plugin\CloudPlugin;
use pocketcloud\util\ActionResult;
use pocketcloud\util\SingletonTrait;
use ReflectionClass;

class EventManager {
    use SingletonTrait;

    private array $handlers = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function registerEvent(string $eventClass, Closure $closure, CloudPlugin $plugin): ActionResult {
        if (is_subclass_of($eventClass, Event::class)) {
            $this->handlers[$plugin->getDescription()->getFullName()][$eventClass][] = $closure;
            return ActionResult::success();
        }
        return ActionResult::failure();
    }

    public function registerListener(Listener $listener, CloudPlugin $plugin): ActionResult {
        $reflection = new ReflectionClass($listener);
        $i = 0;
        foreach ($reflection->getMethods() as $method) {
            if (!$method->isAbstract() && !$method->isStatic() && $method->isPublic() && $method->getNumberOfParameters() == 1) {
                $event = $method->getParameters()[0]->getType()->getName();
                if (is_subclass_of($event, Event::class)) {
                    $i++;
                    $this->handlers[$plugin->getDescription()->getFullName()][$event][] = $method->getClosure($listener);
                }
            }
        }

        return $i > 0 ? ActionResult::success() : ActionResult::failure();
    }

    public function removeHandlers(CloudPlugin $plugin): ActionResult {
        if (isset($this->handlers[$plugin->getDescription()->getFullName()])) {
            unset($this->handlers[$plugin->getDescription()->getFullName()]);
            return ActionResult::success();
        }
        return ActionResult::failure();
    }

    public function removeAll(): void {
        $this->handlers = [];
    }

    public function callEvent(Event $event): void {
        foreach ($this->handlers as $pluginHandler) {
            foreach (($pluginHandler[$event::class] ?? []) as $handler) {
                ($handler)($event);
            }
        }
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}