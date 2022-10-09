<?php

namespace pocketcloud\network\packet\listener;

use pocketcloud\event\Listener;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\utils\SingletonTrait;

class PacketListener {
    use SingletonTrait;

    private array $handlers = [];

    public function register(string $packetClass, \Closure $closure) {
        if (is_subclass_of($packetClass, CloudPacket::class)) {
            $this->handlers[$packetClass][] = $closure;
        }
    }

    public function registerListener(Listener $listener) {
        $reflection = new \ReflectionClass($listener);
        foreach ($reflection->getMethods() as $method) {
            if (!$method->isAbstract() && !$method->isStatic() && $method->isPublic() && $method->getNumberOfParameters() == 1) {
                $packet = $method->getParameters()[0]->getType();
                if ($packet instanceof CloudPacket) $this->handlers[$packet::class][] = $method->getClosure($listener);
            }
        }
    }

    public function call(CloudPacket $packet, ServerClient $client) {
        foreach (($this->handlers[$packet::class] ?? []) as $handler) {
            ($handler)($packet, $client);
        }
    }
}