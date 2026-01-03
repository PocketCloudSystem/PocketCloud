<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class ProxyRegisterServerPacket extends CloudPacket implements ClientboundPacket {

    public function __construct(
        private readonly string $serverName = "",
        private readonly int $port = 0
    ) {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->serverName, $this->port);
    }

    public function decodePayload(PacketData $packetData): void {}

    public function getServerName(): string {
        return $this->serverName;
    }

    public function getPort(): int {
        return $this->port;
    }

    public static function create(string $serverName, int $port): self {
        return new self($serverName, $port);
    }
}