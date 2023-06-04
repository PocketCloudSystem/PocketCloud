<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\utils\PacketData;

class ProxyRegisterServerPacket extends CloudPacket {

    public function __construct(
        private string $serverName = "",
        private int $port = 0
    ) {}

    public function encodePayload(PacketData $packetData) {
        $packetData->write($this->serverName);
        $packetData->write($this->port);
    }

    public function decodePayload(PacketData $packetData) {
        $this->serverName = $packetData->readString();
        $this->port = $packetData->readInt();
    }

    public function getServerName(): string {
        return $this->serverName;
    }

    public function getPort(): int {
        return $this->port;
    }

    public function handle(ServerClient $client) {}
}