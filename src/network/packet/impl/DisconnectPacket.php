<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\ServerDisconnectReason;
use pocketcloud\cloud\network\packet\util\PacketData;

final class DisconnectPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket {

    public function __construct(private ?ServerDisconnectReason $reason = null) {}

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            $server->handleDisconnect();
        }
    }

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeServerDisconnectReason($this->reason);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->reason = $packetData->readServerDisconnectReason();
    }

    public function getReason(): ?ServerDisconnectReason {
        return $this->reason;
    }

    public static function create(ServerDisconnectReason $reason): self {
        return new self($reason);
    }
}