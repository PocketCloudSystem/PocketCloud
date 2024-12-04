<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;

class ProxyUnregisterServerPacket extends CloudPacket {

    public function __construct(private string $serverName = "") {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->serverName);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->serverName = $packetData->readString();
    }

    public function getServerName(): string {
        return $this->serverName;
    }

    public function handle(ServerClient $client): void {}

    public static function create(string $server): self {
        return new self($server);
    }
}