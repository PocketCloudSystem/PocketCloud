<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;

class PlayerConnectPacket extends CloudPacket {

    public function __construct(private ?CloudPlayer $player = null) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writePlayer($this->player);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->player = $packetData->readPlayer();
    }

    public function getPlayer(): ?CloudPlayer{
        return $this->player;
    }

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            if (CloudPlayerManager::getInstance()->get($this->player->getName()) === null) {
                if ($server->getTemplate()->getTemplateType()->isServer()) $this->player->setCurrentServer($server);
                else $this->player->setCurrentProxy($server);
                CloudPlayerManager::getInstance()->add($this->player);
            }
        }
    }

    public static function create(CloudPlayer $player): self {
        return new self($player);
    }
}