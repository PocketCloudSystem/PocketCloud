<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\cache\NotificationListCache;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class PlayerUpdateNotificationStatePacket extends CloudPacket implements CloudboundPacket {

    public function __construct(
        private string $player = "",
        private bool $value = false
    ) {}

    public function handle(ServerClient $client): void {
        if ($this->value) NotificationListCache::add($this->player);
        else NotificationListCache::remove($this->player);
    }

    public function encodePayload(PacketData $packetData): void {}

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->player, $this->value);
    }

    public function getPlayer(): string {
        return $this->player;
    }

    public function isValue(): bool {
        return $this->value;
    }

    public static function create(string $player, bool $value): self {
        return new self($player, $value);
    }
}