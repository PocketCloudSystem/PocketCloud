<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\player\CloudPlayerManager;

final class PlayerDisconnectPacket extends CloudPacket implements CloudboundPacket {

    public function __construct(private ?string $player = null) {}

    public function handle(ServerClient $client): void {
        if (($player = CloudPlayerManager::getInstance()->get($this->player)) !== null) {
            if ($player->getCurrentProxy() === null) {
                CloudPlayerManager::getInstance()->remove($player);
            } else {
                if (($server = $client->getServer()) !== null) {
                    if ($server->getTemplate()->getTemplateType()->isProxy()) {
                        CloudPlayerManager::getInstance()->remove($player);
                    }
                }
            }
        }
    }

    public function encodePayload(PacketData $packetData): void {}

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->player);
    }

    public function getPlayer(): ?string {
        return $this->player;
    }

    public static function create(string $player): self {
        return new self($player);
    }
}