<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class PacketPool {
    use SingletonTrait;

    /** @var array<CloudPacket> */
    private array $packets = [];

    public static function init(): void {
        self::setInstance(new self());
    }

    public function __construct() {
        self::setInstance($this);
    }

    public function register(string $packetClass): void {
        if (!is_subclass_of($packetClass, CloudPacket::class)) return;
        CloudLogger::get()->debug("Registering packet " . basename($packetClass) . " (" . $packetClass . ")");
        $this->packets[basename($packetClass)] = $packetClass;
    }

    public function get(string $pid): ?CloudPacket {
        $get = $this->packets[$pid] ?? null;
        return ($get == null ? null : new $get());
    }

    public function getAll(): array {
        return $this->packets;
    }
}