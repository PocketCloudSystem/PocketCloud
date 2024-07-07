<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\player\CloudPlayerManager;

class PlayerTransferPacket extends CloudPacket {

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
        if (($player = CloudPlayerManager::getInstance()->getPlayerByName($this->player)) !== null) {
            $player->getCurrentProxy()?->sendPacket($this);
        }
    }
}