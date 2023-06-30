<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\utils\PacketData;

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
}