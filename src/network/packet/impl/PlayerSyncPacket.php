<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\player\CloudPlayer;

final class PlayerSyncPacket extends CloudPacket implements ClientboundPacket {

    public function __construct(
        private readonly ?CloudPlayer $player = null,
        private readonly bool $removal = false
    ) {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->player, $this->removal);
    }

    public function decodePayload(PacketData $packetData): void {}

    public function getPlayer(): ?CloudPlayer {
        return $this->player;
    }

    public function isRemoval(): bool {
        return $this->removal;
    }

    public static function create(CloudPlayer $player, bool $removal): self {
        return new self($player, $removal);
    }
}