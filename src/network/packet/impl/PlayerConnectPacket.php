<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;

final class PlayerConnectPacket extends CloudPacket implements CloudboundPacket {

    public function __construct(private ?CloudPlayer $player = null) {}

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            if (CloudPlayerManager::getInstance()->get($this->player->getName()) === null) {
                if ($server->getTemplate()->getTemplateType()->isServer()) $this->player->setCurrentServer($server);
                else $this->player->setCurrentProxy($server);
                CloudPlayerManager::getInstance()->add($this->player);
            }
        }
    }

    public function encodePayload(PacketData $packetData): void {}

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAllTypeSafe([&$this->player], [fn() => $packetData->readPlayer()]);
    }

    public function getPlayer(): ?CloudPlayer {
        return $this->player;
    }

    public static function create(CloudPlayer $player): self {
        return new self($player);
    }
}