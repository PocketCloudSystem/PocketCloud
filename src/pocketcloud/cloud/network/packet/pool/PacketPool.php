<?php

namespace pocketcloud\cloud\network\packet\pool;

use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\impl\normal\KeepAlivePacket;
use pocketcloud\cloud\network\packet\impl\request\ServerHandshakeRequestPacket;
use pocketcloud\cloud\network\packet\impl\response\ServerHandshakeResponsePacket;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\SingletonTrait;
use pocketcloud\cloud\util\Utils;

final class PacketPool {
    use SingletonTrait;

    /** @var array<CloudPacket> */
    private array $packets = [];

    public function __construct() {
        self::setInstance($this);

        self::registerPacket(KeepAlivePacket::class);
        self::registerPacket(ServerHandshakeRequestPacket::class);
        self::registerPacket(ServerHandshakeResponsePacket::class);
    }

    public function registerPacket(string $packetClass): void {
        if (!is_subclass_of($packetClass, CloudPacket::class)) return;
        CloudLogger::get()->debug("Registering packet " . Utils::cleanPath($packetClass, true) . " (" . $packetClass . ")");
        $this->packets[Utils::cleanPath($packetClass, true)] = $packetClass;
    }

    public function getPacketById(string $pid): ?CloudPacket {
        $get = $this->packets[$pid] ?? null;
        return ($get == null ? null : new $get());
    }

    public function getPackets(): array {
        return $this->packets;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}