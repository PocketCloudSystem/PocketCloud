<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\player\CloudPlayerManager;

final class PlayerTransferPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket {

    public function __construct(
        private string $player = "",
        private string $server = ""
    ) {}

    public function handle(ServerClient $client): void {
        if (($player = CloudPlayerManager::getInstance()->get($this->player)) !== null) {
            $player->getCurrentProxy()?->sendPacket($this);
        }
    }

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->player, $this->server);
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->player, $this->server);
    }

    public function getPlayer(): string {
        return $this->player;
    }

    public function getServer(): string {
        return $this->server;
    }

    public static function create(string $player, string $server): self {
        return new self($player, $server);
    }
}