<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\player\CloudPlayerManager;

final class PlayerKickPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket {

    public function __construct(
        private string $player = "",
        private string $reason = "",
        private string $disconnectScreenMessage = ""
    ) {}

    public function handle(ServerClient $client): void {
        if (($player = CloudPlayerManager::getInstance()->get($this->player)) !== null) $player->kick($this->reason, $this->disconnectScreenMessage);
    }

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->player, $this->reason, $this->disconnectScreenMessage);
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->player, $this->reason, $this->disconnectScreenMessage);
    }

    public static function create(string $player, string $reason, string $disconnectScreenMessage): self {
        return new self($player, $reason, $disconnectScreenMessage);
    }
}