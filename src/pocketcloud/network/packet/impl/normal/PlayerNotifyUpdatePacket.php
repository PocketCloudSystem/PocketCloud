<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\config\impl\NotifyList;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\utils\PacketData;

class PlayerNotifyUpdatePacket extends CloudPacket {

    public function __construct(
        private string $playerName = "",
        private bool $value = false
    ) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->playerName);
        $packetData->write($this->value);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->playerName = $packetData->readString();
        $this->value = $packetData->readBool();
    }

    public function getPlayerName(): string {
        return $this->playerName;
    }

    public function getValue(): bool {
        return $this->value;
    }

    public function handle(ServerClient $client): void {
        if ($this->value) NotifyList::add($this->playerName);
        else NotifyList::remove($this->playerName);
    }
}