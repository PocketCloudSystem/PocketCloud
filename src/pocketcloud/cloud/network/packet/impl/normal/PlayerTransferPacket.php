<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\player\CloudPlayerManager;

final class PlayerTransferPacket extends CloudPacket {

    public function __construct(
        private string $player = "",
        private string $server = ""
    ) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->player)->write($this->server);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->player = $packetData->readString();
        $this->server = $packetData->readString();
    }

    public function handle(ServerClient $client): void {
        if (($player = CloudPlayerManager::getInstance()->get($this->player)) !== null) {
            $player->getCurrentProxy()?->sendPacket($this);
        }
    }

    public static function create(string $player, string $server): self {
        return new self($player, $server);
    }
}