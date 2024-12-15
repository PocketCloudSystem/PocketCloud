<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\player\CloudPlayer;

final class PlayerSyncPacket extends CloudPacket {

    public function __construct(
        private ?CloudPlayer $player = null,
        private bool $removal = false
    ) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writePlayer($this->player);
        $packetData->write($this->removal);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->player = $packetData->readPlayer();
        $this->removal = $packetData->readBool();
    }

    public function getPlayer(): ?CloudPlayer {
        return $this->player;
    }

    public function isRemoval(): bool {
        return $this->removal;
    }

    public function handle(ServerClient $client): void {}

    public static function create(CloudPlayer $player, bool $removal): self {
        return new self($player, $removal);
    }
}