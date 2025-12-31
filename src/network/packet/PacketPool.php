<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\network\packet\impl\CloudNotificationPacket;
use pocketcloud\cloud\network\packet\impl\DisconnectPacket;
use pocketcloud\cloud\network\packet\impl\KeepAlivePacket;
use pocketcloud\cloud\network\packet\impl\request\ServerHandshakeRequestPacket;
use pocketcloud\cloud\network\packet\impl\response\ServerHandshakeResponsePacket;
use pocketcloud\cloud\util\trait\SingletonTrait;
use ReflectionClass;

final class PacketPool {
    use SingletonTrait;

    /** @var array<CloudPacket> */
    private array $packets = [];

    public static function init(): void {
        self::setInstance(new self());
    }

    public function __construct() {
        self::setInstance($this);
        $this->register(ServerHandshakeRequestPacket::class);
        $this->register(ServerHandshakeResponsePacket::class);
        $this->register(DisconnectPacket::class);
        $this->register(KeepAlivePacket::class);
        $this->register(CloudNotificationPacket::class);
    }

    public function register(string $packetClass): void {
        if (!is_subclass_of($packetClass, CloudPacket::class)) return;
        CloudLogger::get()->forceDebug("Registering packet " . ($packetName = new ReflectionClass($packetClass)->getShortName()) . " (" . $packetClass . ")");
        $this->packets[$packetName] = $packetClass;
    }

    public function get(string $pid): ?CloudPacket {
        $get = $this->packets[$pid] ?? null;
        return ($get == null ? null : new $get());
    }

    public function getAll(): array {
        return $this->packets;
    }
}