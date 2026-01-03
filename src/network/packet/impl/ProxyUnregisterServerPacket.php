<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class ProxyUnregisterServerPacket extends CloudPacket implements ClientboundPacket {

    public function __construct(private readonly string $serverName = "") {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->serverName);
    }

    public function decodePayload(PacketData $packetData): void {}

    public function getServerName(): string {
        return $this->serverName;
    }

    public static function create(string $serverName): self {
        return new self($serverName);
    }
}