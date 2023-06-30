<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\template\TemplateType;

class PlayerDisconnectPacket extends CloudPacket {

    public function __construct(private ?string $playerName = "") {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->playerName);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->playerName = $packetData->readString();
    }

    public function getPlayer(): string {
        return $this->playerName;
    }

    public function handle(ServerClient $client): void {
        if (($player = CloudPlayerManager::getInstance()->getPlayerByName($this->playerName)) !== null) {
            if ($player->getCurrentProxy() === null) {
                CloudPlayerManager::getInstance()->removePlayer($player);
            } else {
                if (($server = ServerClientManager::getInstance()->getServerOfClient($client)) !== null) {
                    if ($server->getTemplate()->getTemplateType() === TemplateType::PROXY()) {
                        CloudPlayerManager::getInstance()->removePlayer($player);
                    }
                }
            }
        }
    }
}