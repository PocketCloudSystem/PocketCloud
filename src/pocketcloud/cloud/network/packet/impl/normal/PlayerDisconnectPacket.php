<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\player\CloudPlayerManager;

final class PlayerDisconnectPacket extends CloudPacket {

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
        if (($player = CloudPlayerManager::getInstance()->get($this->playerName)) !== null) {
            if ($player->getCurrentProxy() === null) {
                CloudPlayerManager::getInstance()->remove($player);
            } else {
                if (($server = ServerClientCache::getInstance()->getServer($client)) !== null) {
                    if ($server->getTemplate()->getTemplateType()->isProxy()) {
                        CloudPlayerManager::getInstance()->remove($player);
                    }
                }
            }
        }
    }

    public static function create(string $player): self {
        return new self($player);
    }
}