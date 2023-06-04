<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\server\status\ServerStatus;

class CloudServerStatusChangePacket extends CloudPacket {

    public function __construct(private ?ServerStatus $newStatus = null) {}

    public function encodePayload(PacketData $packetData) {
        $packetData->writeServerStatus($this->newStatus);
    }

    public function decodePayload(PacketData $packetData) {
        $this->newStatus = $packetData->readServerStatus();
    }

    public function getNewStatus(): ?ServerStatus {
        return $this->newStatus;
    }

    public function handle(ServerClient $client) {
        if (($server = $client->getServer()) !== null) {
            $server->setServerStatus($this->newStatus);
        }
    }
}