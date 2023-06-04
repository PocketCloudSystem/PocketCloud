<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\player\CloudPlayerManager;

class PlayerKickPacket extends CloudPacket {

    public function __construct(
        private string $playerName = "",
        private string $reason = ""
    ) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->playerName);
        $packetData->write($this->reason);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->playerName = $packetData->readString();
        $this->reason = $packetData->readString();
    }

    public function getPlayerName(): string {
        return $this->playerName;
    }

    public function getReason(): string {
        return $this->reason;
    }

    public function handle(ServerClient $client) {
        if (($player = CloudPlayerManager::getInstance()->getPlayerByName($this->playerName)) !== null) $player->kick($this->reason);
    }
}