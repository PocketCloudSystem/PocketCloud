<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\provider\CloudProvider;

final class PlayerNotifyUpdatePacket extends CloudPacket {

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
        if ($this->value) CloudProvider::current()->enablePlayerNotifications($this->playerName);
        else CloudProvider::current()->disablePlayerNotifications($this->playerName);
    }

    public static function create(string $player, bool $v): self {
        return new self($player, $v);
    }
}